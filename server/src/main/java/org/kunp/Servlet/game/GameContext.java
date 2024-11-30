package org.kunp.Servlet.game;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.kunp.Servlet.session.Session;

//TODO : thread로 따로 빼기
public class GameContext {

  private final Map<String, OutputStream> participants = new ConcurrentHashMap<>();
  private final Map<String, int[]> positions = new ConcurrentHashMap<>();
  private final int gameId;
  private final AtomicBoolean isFinished;
  private Timer gameTimer;

  public GameContext(int gameId, AtomicBoolean isFinished) {
    this.gameId = gameId;
    this.isFinished = isFinished;
  }

  // 새로운 게임 시작 처리 메서드
  public void startGameWithRequest(String sessionId, String roomName, int userLimit, int timeLimit) {
    if (gameTimer != null) {
      throw new IllegalStateException("Game is already started!");
    }

    // 게임 설정
    int adjustedTimeLimit = timeLimit; // 시간 제한 설정
    this.isFinished.set(false);
    gameTimer = new Timer();
    gameTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        if (!isFinished.get()) {
          finishGame(1); // 도망자 승리
        }
      }
    }, adjustedTimeLimit * 1000); // 제한 시간 설정

    // 역할 랜덤 배정
    List<String> participantIds = new ArrayList<>(participants.keySet());
    Collections.shuffle(participantIds); // 참가자 리스트 셔플
    int halfSize = participantIds.size() / 2;

    Random random = new Random();
    for (int i = 0; i < participantIds.size(); i++) {
      String participantId = participantIds.get(i);
      int role = (i < halfSize) ? 0 : 1; // 절반은 술래(0), 나머지는 도망자(1)

      // 시작 위치 생성
      int startX = random.nextInt(100); // 임의의 X 좌표
      int startY = random.nextInt(100); // 임의의 Y 좌표
      positions.put(participantId, new int[] {startX, startY, 0}); // roomNumber는 0으로 설정
      playerStates.put(participantId, false); // 기본 상태는 alive

      // 응답 메시지 생성
      String responseMessage = createStartGameResponse(this.gameId, role, startX, startY);
      try {
        entry.getValue().write(responseMessage.getBytes());
        entry.getValue().flush();
      } catch (IOException e) {
        System.err.println("Error sending start response to " + participantId + ": " + e.getMessage());
      }
    }

    // 게임 시작 응답 메시지 생성
    private String createStartGameResponse(int gameId, int role, int x, int y) {
      return "113|" + gameId + "|" + role + "|" + x + "|" + y + "\n";
    }

    System.out.println("Game " + gameId + " has started in room " + roomName + " with time limit: " + adjustedTimeLimit + " seconds!");
  }

  public boolean isFinished() {
    return this.isFinished.get();
  }


  // 게임 종료 처리
  public synchronized void finishGame(int winner) {
    if (isFinished.get()) return; // 이미 종료된 게임
    this.isFinished.set(true);

    if (gameTimer != null) {
      gameTimer.cancel();
    }

    String resultMessage = createGameResultMessage(winner);
    broadcastMessage(resultMessage);

    System.out.println("Game " + gameId + " has ended! Winner: " + (winner == 0 ? "Chaser" : "Runner"));

    // 리소스 정리
    participants.clear();
    positions.clear();
    playerStates.clear();
    roles.clear();
  }

  // 게임 결과 메시지 생성
  private String createGameResultMessage(int winner) {
    return "213|" + this.gameId + "|" + winner + "\n";
  }

  // 시작 응답 메시지 생성
  private String createStartGameResponse(int gameId, int role, int x, int y) {
    return "113|" + gameId + "|" + role + "|" + x + "|" + y + "\n";
  }

  // 메시지 브로드캐스트
  private void broadcastMessage(String message) {
    for (OutputStream oos : participants.values()) {
      try {
        oos.write(message.getBytes());
        oos.flush();
      } catch (IOException e) {
        System.err.println("Error broadcasting message: " + e.getMessage());
      }
    }
  }


  public void updateContext(String sessionId, int x, int y, int roomId) {
    this.positions.putIfAbsent(sessionId, new int[3]);
    this.positions.get(sessionId)[0] = x;
    this.positions.get(sessionId)[1] = y;
    this.positions.get(sessionId)[2] = roomId;
  }

  public void updateAndBroadCast() {
    for (OutputStream oos : participants.values()) {
      try {
        for(Map.Entry<String, int[]> entry : positions.entrySet()) {
          oos.write(createMessage(1, entry.getValue(), entry.getKey(), this.gameId).getBytes());
          oos.flush();
        }
      } catch (SocketException e) {
        //participants.remove(oos);
      } catch (IOException e) {
        //throw new RuntimeException(e);
      }
    }
  }

  public void enter(Session session) {
    if(participants.containsKey(session.getSessionId())) return;
    participants.put(session.getSessionId(), (OutputStream) session.getAttributes().get("ops"));
  }

  public void leave(Session session) {
    participants.remove(session.getSessionId());
  }

  public boolean isEmpty() {
    return participants.isEmpty();
  }

  private String createMessage(int type, int[] position, String id, int gameId) {
    return type + "|" + id + "|" + position[0] + "|" + position[1] + "|" + position[2] + "|" + gameId+
            "\n";
  }


  // 위치 업데이트와 술래끼리 잡힘 방지
  public void updateInteraction(String id, int roomNumber, boolean isChaser) throws IOException {
    int[] pos = positions.get(id); // 호출한 플레이어의 좌표

    if (isChaser) {
      // 술래인 경우
      for (Map.Entry<String, int[]> entry : positions.entrySet()) {
        String targetId = entry.getKey();

        if (targetId.equals(id) || roles.get(targetId) == 0) continue; // 자신이거나 술래끼리는 무시

        int[] targetPos = entry.getValue();

        if (isAvailable(pos, targetPos) && !Boolean.TRUE.equals(playerStates.get(targetId))) {
          // 도망자가 잡혔을 때 처리
          playerStates.put(targetId, true); // 상태를 '잡힘'으로 업데이트
          System.out.println("Player " + targetId + " has been captured by chaser " + id + ".");

          // 감옥 좌표로 이동 (임시 좌표 설정)
          targetPos[0] = -1; // 감옥 X 좌표 (임의 값)
          targetPos[1] = -1; // 감옥 Y 좌표 (임의 값)
          targetPos[2] = roomNumber; // 감옥 방 번호

          // 잡힌 플레이어에게 메시지 전송
          String captureMessage = createInteractionMessage(211, targetPos, targetId, this.gameId);
          participants.get(targetId).write(captureMessage.getBytes());
          participants.get(targetId).flush();

          // 도망자가 모두 잡혔는지 확인
          if (allRunnersCaptured()) {
            finishGame(0); // 술래 승리
          }
        }

        public void updateInteraction(String id, int roomNumber) throws IOException {
          //TODO : is chaser
          int[] pos = positions.get(id);
          //check interaction
          for(Map.Entry<String, int[]> entry : positions.entrySet()) {
            if(entry.getKey().equals(id)) continue;
            int[] targetPos = entry.getValue();
            System.out.println("pos : " + pos[0] + " " + pos[1]);
            if(isAvailable(pos, targetPos)) {
              //TODO : send message
              System.out.println("dead");;
              participants.get(entry.getKey()).write(createMessage(2, targetPos, entry.getKey(), this.gameId).getBytes());

            }
          }
        }

        // 도망자가 모두 잡혔는지 확인
        private boolean allRunnersCaptured() {
          for (Map.Entry<String, Integer> roleEntry : roles.entrySet()) {
            if (roleEntry.getValue() == 1 && !Boolean.TRUE.equals(playerStates.get(roleEntry.getKey()))) {
              return false; // 아직 잡히지 않은 도망자 존재
            }
          }
          return true;
        }

        // 감옥에 있는지 확인하는 메서드 (임시 구현, 추후 감옥 좌표 설정)
        private boolean isInJail(int[] pos) {
          return pos[0] == -1 && pos[1] == -1; // 감옥 좌표의 임시 기준
        }

        // 응답 메시지 생성 메서드
        private String createInteractionMessage(int type, int[] position, String sessionId, int gameId) {
          return type + "|" + sessionId + "|" + position[0] + "|" + position[1] + "|" + position[2] + "|" + gameId + "\n";
        }

        private boolean isAvailable(int[] pos1, int[] pos2) {
          return pos1[0] - pos2[0] < 10 && pos1[1] - pos2[1] < 10;
        }
      }


