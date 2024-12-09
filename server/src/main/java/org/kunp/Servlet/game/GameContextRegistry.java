package org.kunp.Servlet.game;

import org.kunp.Servlet.message.GameMessage;
import org.kunp.Servlet.session.Session;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GameContextRegistry {

  private static GameContextRegistry registry;
  private AtomicInteger gameId = new AtomicInteger(1);
  private final Map<Integer, GameContext> gameContexts = new ConcurrentHashMap<>();
  private final ExecutorService gameThreadPool = Executors.newCachedThreadPool();

  // Singleton Method
  public static GameContextRegistry getInstance() {
    if(registry == null) {
      registry = new GameContextRegistry();
    }
    return registry;
  }

  // Game Context Method
  public int createGameContext(String roomName, String hostId, int userLimit, int timeLimit) {
    int roomNumber = gameId.getAndAdd(1);
    GameContext gc = new GameContext(roomNumber, new AtomicBoolean(false), userLimit, timeLimit, roomName);
    registerGameContext(roomNumber, gc);
    return roomNumber;
  }

  public void startGameContext(int gameId) {
    GameContext gc = gameContexts.get(gameId);
    gc.setChasersAndStart();
    Runnable gameThread = new GameThread(gc);
    this.gameThreadPool.submit(gameThread);
  }

  public boolean hasGame(int gameId) {
    return gameContexts.containsKey(gameId);
  }

  public void updatePositionState(GameMessage message) {
    GameContext gc = gameContexts.get(message.getGameId());
    if(gc == null) return;
    gc.updatePositionContext(message.getId(), message.getX(), message.getY(), message.getRoomNumber());
  }

  public void subscribe(Session session, int gameId) {
    GameContext gc = gameContexts.get(gameId);
    if(gc == null) return;
    gc.enter(session);
  }

  public void unsubscribe(Session session, int roomNumber) {
    GameContext gc = gameContexts.get(roomNumber);
    if(gc.isEmpty()) {
      gameContexts.remove(roomNumber);
    }
  }

  public void endGame(int gameId) {
    gameContexts.remove(gameId);
  }

  private void registerGameContext(int roomNumber, GameContext gc) {
    gameContexts.put(roomNumber, gc);
  }

  public void interact(GameMessage message) throws IOException {
    GameContext gc = gameContexts.get(message.getGameId());
    if(gc == null) return;

    gc.handleInteractions(message.getId(), message.getRoomNumber());
  }

  public void monitor() {
    System.out.println("GameContextRegistry is monitoring");
    gameContexts.entrySet().stream()
        .forEach(e ->{
            System.out.println("GameContext: " + e.getKey() + " " + e.getValue().isFinished());
        });
  }
}
