package org.kunp.Servlet.menu;

import org.kunp.Servlet.game.GameContextRegistry;
import org.kunp.Servlet.game.GameRequestHandler;
import org.kunp.Servlet.session.Session;
import org.kunp.Servlet.session.SessionManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WaitingRoomContext {
  private final Map<String, OutputStream> participants = new ConcurrentHashMap<>();
  private final Set<String> cancelList = new HashSet<>();
  private final String roomName;
  private int gameId;
  private String hostId;
  private final int userLimit;
  private final int timeLimit;
  private boolean exposeLevel = true;

  public WaitingRoomContext(String roomName, String hostId, int userLimit, int timeLimit) {
    // 방 이름 검증
    if (roomName.contains(",")) {
      throw new IllegalArgumentException("Room name cannot contain ','");
    }
    this.roomName = roomName;
    this.hostId = hostId;
    this.userLimit = userLimit;
    this.timeLimit = timeLimit;
  }

  private void disconnectParticipants() {
    cancelList.forEach(this::leave);
    cancelList.clear();
  }

  // 110번: 입장 메세지
  public synchronized void enter(Session session) throws IOException {

    if (participants.size() >= userLimit) {
      return;
    }

    OutputStream ops = (OutputStream) session.getAttributes().get("ops");
    participants.put(session.getSessionId(), ops);
    // Broadcast to all participants
    for (Map.Entry<String, OutputStream> entry : participants.entrySet()) {
      broadCast("110|%s|%s|content|time\n".formatted(entry.getKey(), roomName));
    }
    disconnectParticipants();
    // Send updated participant list to the new user
    ops.write("110|%s|%s|content|time\n".formatted(String.join(",", participants.keySet()), roomName).getBytes());
    ops.flush();
  }

  // 111번: 퇴장 메세지
  public void leave(String sessionId) {
    synchronized (this) {
      participants.remove(sessionId);
      if(participants.isEmpty()) {
        WaitingRoomRegistry.getInstance().removeWaitingRoom(roomName);
        return;
      }
      if(sessionId.equals(hostId)) {
        //select random host
        hostId = participants.keySet().iterator().next();
      }
    }
    broadCast("111|%s\n".formatted(sessionId));
  }

  public void broadCast(String message) {
    for (Map.Entry<String, OutputStream> entry : participants.entrySet()) {
      try {
        OutputStream oos = entry.getValue();
        oos.write(message.getBytes());
        oos.flush();
      } catch (SocketException e) {
        System.err.println("SocketException: " + e.getMessage());
        cancelList.add(entry.getKey());
      } catch (IOException e) {
        System.err.println("IOException: " + e.getMessage());
        cancelList.add(entry.getKey());
      }
    }
  }

  public boolean isFull() {
    return participants.size() >= userLimit;
  }

  public boolean isHost(String sessionId) {
    return hostId.equals(sessionId);
  }

  public boolean isClosed() {
    return exposeLevel;
  }

  public String getRoomName() {
    return roomName;
  }

  public int getUserLimit() {
    return userLimit;
  }

  public int getTimeLimit() {
    return timeLimit;
  }

  public String getHostId() {
    return hostId;
  }

  public Map<String, OutputStream> getParticipants() {
    return participants;
  }

  // 게임 초기화
  public void initGame(Session session) {
    boolean allParticipants = true;
    disconnectParticipants();
    if(!hostId.equals(session.getSessionId())) {
      return;
    }

    if (participants.size() < 2) {
      return;
    }

    for(Map.Entry<String, OutputStream> entry : participants.entrySet()) {
      if(SessionManager.getInstance().getSession(entry.getKey()) == null) {
        cancelList.add(entry.getKey());
        allParticipants = false;
      }
    }
    if(allParticipants) {
      this.gameId = GameRequestHandler.getInstance().createGameContextAndJoinAll(this);
      this.exposeLevel = false;
    } else {
      cancelList.forEach(participants::remove);
      cancelList.forEach(e -> broadCast("111|%s\n".formatted(e)));
      cancelList.clear();
    }
  }

  public void endGame() {
    this.exposeLevel = true;
    GameContextRegistry.getInstance().endGame(gameId);
    disconnectParticipants();
    // Broadcast to all participants
    for (Map.Entry<String, OutputStream> entry : participants.entrySet()) {
      broadCast("110|%s|%s|content|time\n".formatted(String.join(",", participants.keySet()), roomName));
    }
  }
}
