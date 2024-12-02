package org.kunp.Servlet.game;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.kunp.Servlet.session.Session;

public class GameContext {

  private final Map<String, OutputStream> participants = new ConcurrentHashMap<>();
  private final Map<String, int[]> positions = new ConcurrentHashMap<>();
  private final Map<String, Integer> isChaser = new HashMap<>();
  private final Set<String> toRemove = new HashSet<>();

  private final int gameId;
  private final AtomicBoolean isStarted = new AtomicBoolean(false);
  private final AtomicBoolean isFinished;

  public GameContext(int gameId, AtomicBoolean isFinished) {
    this.gameId = gameId;
    this.isFinished = isFinished;
  }

  public boolean isFinished() {
    return this.isFinished.get();
  }

  public void updateContext(String sessionId, int x, int y, int roomId) {
    if (!isStarted.get()) return;
    this.positions.putIfAbsent(sessionId, new int[4]);
    this.positions.get(sessionId)[0] = x;
    this.positions.get(sessionId)[1] = y;
    this.positions.get(sessionId)[2] = roomId;
    this.positions.get(sessionId)[3] = isChaser.getOrDefault(sessionId, 1);
  }

  public void updateAndBroadCast() {
    for (Map.Entry<String, OutputStream> participant : participants.entrySet()) {
      try {
        OutputStream oos = participant.getValue();
        for (Map.Entry<String, int[]> entry : positions.entrySet()) {
          oos.write(createMessage(210, entry.getValue(), entry.getKey(), this.gameId).getBytes());
          oos.flush();
        }
      } catch (IOException e) {
        toRemove.add(participant.getKey());
      }
    }
    removeDisconnectedParticipants();
  }

  public void enter(Session session) {
    if(participants.containsKey(session.getSessionId())) return;
    participants.put(session.getSessionId(), (OutputStream) session.getAttributes().get("ops"));
    positions.put(session.getSessionId(), new int[4]);
  }

  public void leave(Session session) {
    participants.remove(session.getSessionId());
  }

  public void setChasers() {
    List<String> keys = new ArrayList<>(participants.keySet());
    if (keys.size() < 2) {
      throw new IllegalStateException("Not enough participants to select two chasers");
    }
    Random random = new Random();
    String chaser1 = keys.get(random.nextInt(keys.size()));
    String chaser2;
    do {
      chaser2 = keys.get(random.nextInt(keys.size()));
    } while (chaser1.equals(chaser2));
    setChaser(chaser1);
    setChaser(chaser2);
    for (Map.Entry<String, OutputStream> entry : participants.entrySet()) {
      try {
        entry.getValue().write(String.format("113|%d|%d|%d\n", isChaser.getOrDefault(entry.getKey(), 1), positions.get(entry.getKey())[0], positions.get(entry.getKey())[1]).getBytes());
        entry.getValue().flush();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    this.isStarted.set(true);
  }

  private void setChaser(String sessionId) {
    isChaser.put(sessionId, 0);
  }

  public boolean isEmpty() {
    return participants.isEmpty();
  }

  private String createMessage(int type, int[] position, String id, int gameId) {
    return type + "|" + id + "|" + position[0] + "|" + position[1] + "|" + position[2] + "|" + gameId+ "|" + position[3] + "\n";
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
        participants.get(entry.getKey()).write(createMessage(211, targetPos, entry.getKey(), this.gameId).getBytes());
      }
    }
  }

  private boolean isAvailable(int[] pos1, int[] pos2) {
    return pos1[0] - pos2[0] < 10 && pos1[1] - pos2[1] < 10;
  }

  private void removeDisconnectedParticipants() {
    for (String key : toRemove) {
      for (Map.Entry<String, OutputStream> entry : participants.entrySet()) {
        try {
          entry.getValue().write(String.format("214|%s\n", key).getBytes());
          entry.getValue().flush();
        } catch (IOException e) {
          e.printStackTrace();
        }
        participants.remove(key);
        positions.remove(key);
      }
    }
    toRemove.clear();
  }
}


