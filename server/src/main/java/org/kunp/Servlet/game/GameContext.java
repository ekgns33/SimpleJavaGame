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
  private final Map<String, Boolean> playerStates = new ConcurrentHashMap<>(); // 플레이어 상태 관리
  private final int gameId;
  private final AtomicBoolean isFinished;

  public GameContext(int gameId, AtomicBoolean isFinished) {
    this.gameId = gameId;
    this.isFinished = isFinished;
  }

  public boolean isFinished() {
    return this.isFinished.get();
  }

  public void updateContext(String sessionId, int x, int y, int roomId) {
    this.positions.putIfAbsent(sessionId, new int[3]);

    // 플레이어가 dead 상태라면 움직임 제한
    if (Boolean.TRUE.equals(playerStates.get(sessionId))) {
      System.out.println("Player " + sessionId + " is dead and cannot move.");
      return;
    }

    // 위치 업데이트
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
    if (participants.containsKey(session.getSessionId())) return;

    participants.put(session.getSessionId(), (OutputStream) session.getAttributes().get("ops"));
    playerStates.put(session.getSessionId(), false); // 기본 상태는 alive
  }


  public void leave(Session session) {
    participants.remove(session.getSessionId());
    playerStates.remove(session.getSessionId()); // 상태 제거
  }


  public boolean isEmpty() {
    return participants.isEmpty();
  }

  private String createMessage(int type, int[] position, String id, int gameId) {
    return type + "|" + id + "|" + position[0] + "|" + position[1] + "|" + position[2] + "|" + gameId+
    "\n";
  }

  public void updateInteraction(String id, int roomNumber, boolean isChaser) throws IOException {
    int[] pos = positions.get(id); // 호출한 플레이어의 좌표

    if (isChaser) {
      // 술래인 경우
      for (Map.Entry<String, int[]> entry : positions.entrySet()) {
        if (entry.getKey().equals(id)) continue; // 자신 제외

        int[] targetPos = entry.getValue();

        if (isAvailable(pos, targetPos) && !Boolean.TRUE.equals(playerStates.get(entry.getKey()))) {
          // 도망자가 잡혔을 때 처리
          playerStates.put(entry.getKey(), true); // 상태를 '잡힘'으로 업데이트
          System.out.println("Player " + entry.getKey() + " has been captured by chaser " + id + ".");

          // 감옥 좌표로 이동 (임시 좌표 설정)
          targetPos[0] = -1; // 감옥 X 좌표 (임의 값)
          targetPos[1] = -1; // 감옥 Y 좌표 (임의 값)
          targetPos[2] = roomNumber; // 감옥 방 번호

          // 잡힌 플레이어에게 메시지 전송
          String captureMessage = createInteractionMessage(211, entry.getKey(), targetPos, this.gameId);
          participants.get(entry.getKey()).write(captureMessage.getBytes());
          participants.get(entry.getKey()).flush();
        }
      }
    } else {
      // 도망자인 경우
      for (Map.Entry<String, int[]> entry : positions.entrySet()) {
        int[] targetPos = entry.getValue();

        if (isAvailable(pos, targetPos) && isInJail(targetPos)) {
          // 감옥 탈출 조건 충족 시
          System.out.println("Player " + id + " helped players escape the jail.");

          for (Map.Entry<String, Boolean> stateEntry : playerStates.entrySet()) {
            if (stateEntry.getValue()) {
              // 잡힌 상태였던 플레이어를 감옥에서 탈출
              stateEntry.setValue(false); // 상태를 '생존'으로 업데이트
              int[] prisonerPos = positions.get(stateEntry.getKey());

              // 탈출한 플레이어 위치를 감옥 밖으로 이동 (임시 좌표 설정)
              prisonerPos[0] = 0; // 탈출 후 기본 위치 X
              prisonerPos[1] = 0; // 탈출 후 기본 위치 Y
              prisonerPos[2] = roomNumber; // 방 번호 유지

              // 탈출한 플레이어에게 메시지 전송
              String escapeMessage = createInteractionMessage(212, stateEntry.getKey(), prisonerPos, this.gameId);
              participants.get(stateEntry.getKey()).write(escapeMessage.getBytes());
              participants.get(stateEntry.getKey()).flush();
            }
          }
        }
      }
    }
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


