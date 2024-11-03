package org.kunp.Servlet;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.kunp.Main;
import org.kunp.Servlet.session.Session;
import org.kunp.Servlet.session.SessionManager;

/**
 * Reactor class to handle incoming connections and dispatch them to the appropriate handler using a
 * thread pool.
 *
 * <p>This is an initial implementation of the Reactor pattern.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Reactor_pattern">Reactor pattern</a>
 * @see ExecutorService
 */
public class Reactor extends Thread {

  private final ServerSocket serverSocket;
  private final ConnectionConfigurer connectionConfigurer;
  private final ExecutorService threadPool;
  private final SessionManager sessionManager;

  public Reactor(
          ServerSocket serverSocket, ConnectionConfigurer connectionConfigurer, int poolSize, SessionManager sessionManager) {
    this.serverSocket = serverSocket;
    this.connectionConfigurer = connectionConfigurer;
    this.threadPool = Executors.newFixedThreadPool(poolSize);
    this.sessionManager = sessionManager;
  }

  @Override
  public void run() {
    waitForConnection();
  }

  private void waitForConnection() {
    while (true) {
      try {
        Socket socket = serverSocket.accept();

        // 세션 생성 및 저장 - SessionManager의 createSession 메서드 사용
        Session clientSession = sessionManager.createSession();
        getGameContext().enter(clientSession);
        System.out.println("Client connected with ID: " + clientSession.getSessionId());

        // 클라이언트 핸들러 생성 후 스레드 풀에서 실행
        Runnable clientHandler = connectionConfigurer.configure(socket);
        threadPool.execute(clientHandler);

      } catch (IOException e) {
        throw new RuntimeException("Error occurred while waiting for connection", e);
      }
    }
  }
}
