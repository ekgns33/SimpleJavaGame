package org.kunp.Servlet.menu;

import org.kunp.Servlet.session.Session;

import java.io.IOException;
import java.util.List;
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
        .map(WaitingRoomContext::getRoomName)
        .collect(Collectors.joining(","));
  }

  // 대기방 입장
  void enterWaitingRoom(Session session, String roomName) throws IOException {
    waitingRooms.putIfAbsent(roomName, new WaitingRoomContext(roomName, session.getSessionId()));
    WaitingRoomContext waitingRoom = waitingRooms.get(roomName);
    waitingRoom.enter(session);
  }

  // 대기방 퇴장
  void leaveWaitingRoom(Session session, String roomName) {
    WaitingRoomContext waitingRoom = waitingRooms.get(roomName);
    waitingRoom.leave(session);
  }

  // 대기방 생성
  void createWaitingRoom(Session session, String roomName) {
    WaitingRoomContext waitingRoom = new WaitingRoomContext(roomName, session.getSessionId());
    waitingRooms.put(roomName, waitingRoom);
  }

  public void startGame(Session session, String roomName) {
    if(!waitingRooms.get(roomName).isHost(session.getSessionId())) {
      return;
    }
    waitingRooms.get(roomName).initGame(session);
  }
}
