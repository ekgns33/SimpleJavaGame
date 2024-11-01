package org.kunp.Servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.kunp.Main;
import org.kunp.Servlet.session.Session;

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

  public Reactor(
          ServerSocket serverSocket, ConnectionConfigurer connectionConfigurer, int poolSize) {
    this.serverSocket = serverSocket;
    this.connectionConfigurer = connectionConfigurer;
    this.threadPool = Executors.newFixedThreadPool(poolSize);
  }

  @Override
  public void run() {
    waitForConnection();
  }

  private void waitForConnection() {
    while (true) {
      try {
        Socket socket = serverSocket.accept();

        // 클라이언트로부터 ID를 첫 메시지로 받는 로직 추가
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String clientId = in.readLine();

        if (clientId != null && !clientId.isEmpty()) {
          // 세션 생성 및 저장
          Session clientSession = new Session(clientId, new HashMap<>(), System.currentTimeMillis(), System.currentTimeMillis());
          Main.clientSessions.put(clientId, clientSession);  // Main 클래스의 clientSessions에 저장
          System.out.println("Client connected with ID: " + clientId);

          // 클라이언트 핸들러 생성 후 스레드 풀에서 실행
          Runnable clientHandler = connectionConfigurer.configure(socket);
          threadPool.execute(clientHandler);
        } else {
          System.out.println("Client connection rejected due to empty ID.");
          socket.close();
        }

      } catch (IOException e) {
        throw new RuntimeException("Error occurred while waiting for connection", e);
      }
    }
  }
}
