package org.kunp.Servlet.game;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.kunp.ResponseCode;
import org.kunp.Servlet.menu.WaitingRoomRegistry;
import org.kunp.Servlet.session.Session;

import static java.lang.String.format;
import static org.kunp.ResponseCode.*;

public class GameContext {
  private static final Logger log = Logger.getGlobal();
  private static final int CHASER = 0;
  private static final int RUNNER = 1;
  private static final int JAIL_X = 0; // 감옥 x 좌표
  private static final int JAIL_Y = 0; // 감옥 y 좌표
  private static final int TOUCHDOWN_X = 70;
  private static final int TOUCHDOWN_Y = 60;
  private static final int ESCAPE_X = 0;
  private static final int ESCAPE_Y = 70;
  private static final int JAIL_ROOM_NUMBER = 1; // 감옥 roomNumber

  // Connection 관련 객체
  private final Map<String, OutputStream> participants = new ConcurrentHashMap<>();
  private final Set<String> cancelList = new HashSet<>(); // Disconnect 대상

  // 게임 상태 관련 객체
  private final Map<String, int[]> positions = new ConcurrentHashMap<>();
  private final Map<String, Integer> isChaser = new HashMap<>();
  private final Map<String, Boolean> playerStates = new ConcurrentHashMap<>(); // Captured은 true

  // 게임 정보
  private final int gameId;
  private final String roomName;
  private final AtomicBoolean isStarted;
  private final AtomicBoolean isFinished;
  private final int timeLimit;
  private LocalDateTime endTime;

  public GameContext(int gameId, AtomicBoolean isFinished, int timeLimit, int userlimit, String roomName) {
    this.gameId = gameId;
    this.roomName = roomName;
    this.isStarted= new AtomicBoolean(false);
    this.isFinished = isFinished;
    this.timeLimit = timeLimit;
  }

  ///////////////// 위치 정보 업데이트 //////////////////////
  /**
   * 클라이언트의 위치 정보를 업데이트하고, 상호작용을 처리한다.
   * @param sessionId : 요청한 클라이언트 세션아이디
   * @param x         : 클라이언트의 x 좌표
   * @param y         : 클라이언트의 y 좌표
   * @param roomId    : 클라이언트의 방 번호
   * */
  public void updatePositionContext(String sessionId, int x, int y, int roomId) {
    // 감옥에 갇힌 상태라면 업데이트 차단
    if (isPlayerDisabled(sessionId)) {
      log.info(format("gameId : %d | Player %s is in jail and cannot move.", this.gameId, sessionId));
      return;
    }
    // 위치 정보 업데이트
    updateClientPosition(sessionId, x, y, roomId, isChaser.getOrDefault(sessionId, RUNNER));
  }

  /***
   * 모든 참가자에게 현재 게임 상태를 업데이트하고 브로드캐스트한다.
   * {@link GameThread#run()}에서 주기적으로 호출된다.
   * 참여자가 Disconnect 되면 해당 참여자를 방에서 퇴장시킨다.
   */
  public void updateAndBroadCast() {
    determineWinner();
    broadcastPositions();
    removeDisconnectedParticipants();
  }

  //////////////////// 상호작용 처리 ///////////////////////

  /***
   * 클라이언트의 상호작용을 처리한다.
   * @param sessionId : 요청한 클라이언트 세션아이디
   * @param roomNumber : 클라이언트의 방 번호
   * */
  public void handleInteractions(final String sessionId, final int roomNumber) throws IOException {
    int[] pos = positions.get(sessionId);
    if (pos == null || isPlayerDisabled(sessionId)) return; // 유효하지 않은 사용자 무시
    if (isRunner(sessionId)) { // 도망자
      handleRunnerInteraction(sessionId, roomNumber);
    } else { // 술래
      handleChaserInteraction(sessionId, roomNumber);
    }
  }

  //////////////////// 게임 관리 ///////////////////////
  /***
   * 새로운 참가자가 게임에 입장한다.
   * @param session Session 객체
   */
  public void enter(Session session) {
    if(participants.containsKey(session.getSessionId())) return;
    initPlayerStateInfo(session);
  }

  /***
   * 게임 시작 시 술래를 설정하고 게임을 시작한다.
   */
  public void setChasersAndStart() {
    List<String> keys = new ArrayList<>(participants.keySet());
    /* 전체 플레이어의 절반을 술래, 절반을 도망자로 배정 */
    int numPlayers = keys.size();
    int numChasers = numPlayers / 2;
    Random random = new Random();
    Set<String> chasers = new HashSet<>();
    while(chasers.size() < numChasers) {
      String chaser = keys.get(random.nextInt(keys.size()));
      chasers.add(chaser);
    }
    // 선택된 술래들을 설정
    chasers.forEach(this::setChaser);
    broadcastPlayerRoles();
    this.isStarted.set(true);
    startTimer(); // 타이머 시작
  }

  public boolean isFinished() {
    return this.isFinished.get();
  }

  public boolean isEmpty() {
    return participants.isEmpty();
  }

  //////////////////// Private 메소드  ///////////////////////

  private void initPlayerStateInfo(Session session) {
    participants.put(session.getSessionId(), (OutputStream) session.getAttributes().get("ops"));
    positions.put(session.getSessionId(), new int[4]);
    isChaser.put(session.getSessionId(), RUNNER);
    playerStates.put(session.getSessionId(), false);
  }


  ////////////////// 통신 관련 메소드 ///////////////////////

  private void broadcastPositions() {
    if(isFinished()) return;
    for (Map.Entry<String, OutputStream> participant : participants.entrySet()) {
      try {
        unicastEntireClientPositions(participant.getValue());
      } catch (IOException e) {
        e.printStackTrace(System.out);
        cancelList.add(participant.getKey());
      }
    }
  }

  private void broadcastPlayerRoles() {
    for (Map.Entry<String, OutputStream> entry : participants.entrySet()) {
      try {
        entry.getValue().write(createGameStartMessage(gameId, isChaser.getOrDefault(entry.getKey(), RUNNER), positions.get(entry.getKey())[0], positions.get(entry.getKey())[1]).getBytes());
        entry.getValue().flush();
      } catch (IOException e) {
        cancelList.add(entry.getKey());
        throw new RuntimeException(e);
      }
    }
  }

  private void unicastEntireClientPositions(OutputStream oos) throws IOException {
    for (Map.Entry<String, int[]> entry : positions.entrySet()) {
      oos.write(createPositionUpdateMessage(entry.getValue(), entry.getKey(), this.gameId).getBytes());
      oos.flush();
    }
  }

  private void broadcastGameResult(boolean chaserWon) {
    int win = chaserWon ? 0 : 1; // 0: 술래 승리, 1: 도망 승리
    String resultMessage = createGameResultMessage(gameId, win);
    sendMessageToAll(resultMessage);
    endGame();
  }

  private void endGame() {
    isFinished.set(true);
    WaitingRoomRegistry.getInstance().endGame(roomName);
  }

  private String createGameResultMessage(int gameId, int win) {
    return format("%d|%d|%d\n", GAME_END.getCode(), gameId, win);
  }

  private String createCaughtResponse(String sessionId,int roomNumber, int gameId, int role) {
    return format("%d|%s|%d|%d|%d|%d|%d\n", GAME_PLAYER_CATCH.getCode(), sessionId, JAIL_X, JAIL_Y, roomNumber, gameId, role);
  }

  private String createFreedResponse(String sessionId, int x, int y, int roomNumber, int gameId, int role) {
    return format("%d|%s|%d|%d|%d|%d|%d\n", GAME_PLAYER_ESCAPE.getCode(), sessionId, x, y, roomNumber, gameId, role);
  }

  private String createLeaveResponse(String sessionId) {
    return format("%d|%s\n", GAME_PLAYER_LEAVE.getCode(), sessionId);
  }

  private String createGameStartMessage(int gameId, int gameRole, int x, int y) {
    return format("%d|%d|%d|%d|%d\n", GAME_START.getCode(), gameId, gameRole, x, y);
  }

  private String createPositionUpdateMessage(int[] position, String id, int gameId) {
    return ResponseCode.GAME_PLAYER_MOVE.getCode() + "|" + id + "|" + position[0] + "|" + position[1] + "|" + position[2] + "|" + gameId+ "|" + position[3] + "\n";
  }

  private void sendMessageToAll(String message) {
    for (Map.Entry<String, OutputStream> entry  : participants.entrySet()) {
      try {
        OutputStream outputStream = entry.getValue();
        outputStream.write(message.getBytes());
        outputStream.flush();
      } catch (IOException e) {
        cancelList.add(entry.getKey());
      }
    }
  }

  /***
   * Disconnect 된 참가자를 제거한다.
   */
  private void removeDisconnectedParticipants() {
    for (String disconnectedClientId : cancelList) {
      for (Map.Entry<String, OutputStream> entry : participants.entrySet()) {
        if(entry.getKey().equals(disconnectedClientId)) continue;
        try {
          entry.getValue().write(createLeaveResponse(disconnectedClientId).getBytes());
          entry.getValue().flush();
        } catch (IOException e) {
          log.severe(format("gameId : %d | Failed to remove disconnected participant %s", this.gameId, disconnectedClientId));
        }
        reap(disconnectedClientId);
      }
    }
    cancelList.clear();
  }

  //////////////////// 게임 로직 ///////////////////////

  private boolean isAvailable(int[] pos1, int[] pos2) {
    int dx = pos1[0] - pos2[0];
    int dy = pos1[1] - pos2[1];
    return (dx * dx + dy * dy) < (20 * 20); // 반지름 5 이내인지 확인
  }


  private void setChaser(String sessionId) {
    isChaser.put(sessionId, CHASER);
  }

  private boolean isPlayerDisabled(String sessionId) {
    return this.playerStates.get(sessionId);
  }

  private void updateClientPosition(String sessionId, int x, int y, int roomId, int role) {

    this.positions.putIfAbsent(sessionId, new int[4]);
    this.positions.get(sessionId)[0] = x;
    this.positions.get(sessionId)[1] = y;
    this.positions.get(sessionId)[2] = roomId;
    this.positions.get(sessionId)[3] = role;
  }

  private void handleRunnerInteraction(String sessionId, int roomNumber) throws IOException {
    int[] runnerPos = positions.get(sessionId);
    if(isNearTouchDown(runnerPos)) {
      releaseRunnersInJail(roomNumber);
    }
  }

  private void releaseRunnersInJail(int roomNumber) {
    playerStates.entrySet().stream()
        .filter(Map.Entry::getValue)
        .forEach(e -> {
          playerStates.put(e.getKey(), false);
          int[] targetPos = positions.get(e.getKey());
          positions.put(e.getKey(), new int[]{0, 50, roomNumber, targetPos[3]}); // 감옥 위치
          String response = createFreedResponse(e.getKey(), ESCAPE_X, ESCAPE_Y, roomNumber, gameId, targetPos[3]);
          sendMessageToAll(response);
        });
  }

  private void handleChaserInteraction(String id, int roomNumber) throws IOException {
    int[] chaserPos = positions.get(id);
    if (chaserPos == null) return;

    for (Map.Entry<String, int[]> entry : positions.entrySet()) {
      String targetId = entry.getKey();
      int[] targetPos = entry.getValue();

      // 술래거나 잡힌 사용자 제외
      if (!isRunner(targetId) || isPlayerDisabled(targetId)) continue;

      // 같은 방에 있는지 확인
      if (chaserPos[2] != targetPos[2] || chaserPos[2] != roomNumber) continue;

      // 도망자가 범위 내에 있는지 확인
      if (isAvailable(chaserPos, targetPos)) {
        // 도망자를 감옥으로 이동
        positions.put(targetId, new int[]{JAIL_X, JAIL_Y, JAIL_ROOM_NUMBER, targetPos[3]}); // 감옥 위치
        playerStates.put(targetId, true); // 상태 업데이트 (갇힌 상태)
        sendMessageToAll(createCaughtResponse(targetId, JAIL_ROOM_NUMBER, gameId, targetPos[3]));
      }
    }
  }


  private boolean isNearTouchDown(int[] runnerPos) {
    // 감옥 근처에 있는지 확인 (JAIL_X, JAIL_Y 기준)
    return Math.abs(runnerPos[0] - TOUCHDOWN_X) < 10 && Math.abs(runnerPos[1] - TOUCHDOWN_Y) < 10;
  }

  public void determineWinner() {
    if (participants.isEmpty()) endGame();
    if (isTimeOut()) {
      // 타임아웃이 된 경우, 도망자 승리
      log.warning(format("gameId : %d | Game timed out. Runners win.", this.gameId));
      broadcastGameResult(false);
    } else {
      boolean allRunnersCaptured = true;
      // 술래가 모든 도망자를 잡았는지 확인
      for (Map.Entry<String, Boolean> entry : playerStates.entrySet()) {
        String playerId = entry.getKey();

        // 플레이어가 도망자인지 확인하고 잡혔는지 검사
        if (isRunner(playerId) && !isPlayerDisabled(entry.getKey())) {
          // 잡히지 않은 도망자가 있는 경우
          allRunnersCaptured = false;
          //log.info(format("gameId : %d | Runner %s is not captured yet.", this.gameId, playerId));
          break; // 추가 검사 불필요, 즉시 탈출
        }
      }
      if (allRunnersCaptured) {
        // 모든 도망자가 잡혔으면 술래가 승리
        broadcastGameResult(true); // 술래 승리
        log.info(format("gameId : %d | All runners captured. Chasers win.", this.gameId));
      }
    }
  }
  private boolean isRunner(String sessionId) {
    return isChaser.get(sessionId) == RUNNER;
  }

  private void reap(String sessionId) {
    WaitingRoomRegistry.getInstance().leaveWaitingRoom(sessionId, roomName);
    playerStates.remove(sessionId);
    isChaser.remove(sessionId);
    positions.remove(sessionId);
    participants.remove(sessionId);
  }

  private void startTimer() {
    LocalDateTime startTime = LocalDateTime.now();  // 게임 시작 시간 기록
    endTime = startTime.plusSeconds(timeLimit*10);  // 게임 종료 시간 계산
  }

  // 타임아웃 체크
  private boolean isTimeOut() {
    if (endTime != null) {
      // 현재 시간이 종료 시간을 넘었는지 확인
      return Duration.between(LocalDateTime.now(), endTime).isNegative();
    }
    return false;
  }
}


