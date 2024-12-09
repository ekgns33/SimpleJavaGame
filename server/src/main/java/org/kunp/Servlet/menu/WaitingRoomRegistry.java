package org.kunp.Servlet.menu;

import org.kunp.Servlet.session.Session;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WaitingRoomRegistry {

  private static WaitingRoomRegistry waitingRoomRegistry;

  private WaitingRoomRegistry() {
  }

  public static WaitingRoomRegistry getInstance() {
    if (waitingRoomRegistry == null) {
      waitingRoomRegistry = new WaitingRoomRegistry();
    }
    return waitingRoomRegistry;
  }

  private final Map<String, WaitingRoomContext> waitingRooms = new ConcurrentHashMap<>();

  // 대기방 조회
  String getWaitingRooms() {
    return waitingRooms.values().stream()
        .filter(WaitingRoomContext::isOpen)
        .map(WaitingRoomContext::getRoomName)
        .collect(Collectors.joining(","));
  }

  // 대기방 입장
  void enterWaitingRoom(Session session, String roomName, int userLimit, int timeLimit) throws IOException {
    waitingRooms.putIfAbsent(roomName, new WaitingRoomContext(roomName, session.getSessionId(), userLimit, timeLimit));
    WaitingRoomContext waitingRoom = waitingRooms.get(roomName);
    if (!waitingRoom.isOpen() || waitingRoom.isFull()) {
      return;
    }
    waitingRoom.enter(session);
  }

  // 대기방 퇴장
  public void leaveWaitingRoom(String sessionId, String roomName) {
    WaitingRoomContext waitingRoom = waitingRooms.get(roomName);
    waitingRoom.leave(sessionId);
  }

  // 대기방 생성
  void createWaitingRoom(Session session, String roomName, int userLimit, int timeLimit) {
    WaitingRoomContext waitingRoom = new WaitingRoomContext(roomName, session.getSessionId(), userLimit, timeLimit);
    waitingRooms.put(roomName, waitingRoom);
  }

  public void removeWaitingRoom(String roomName) {
    waitingRooms.remove(roomName);
  }

  public void startGame(Session session, String roomName, int userLimit, int timeLimit) {

    waitingRooms.get(roomName).initGame(session);
  }

  public void endGame(String roomName) {
    waitingRooms.get(roomName).endGame();
  }
}
