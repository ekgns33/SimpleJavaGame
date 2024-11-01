package org.kunp.Servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.kunp.Servlet.message.Message;
import org.kunp.Servlet.session.Session;

//TODO : thread로 따로 빼기
public class GameContext {

  private final Map<String, OutputStream> participants = new ConcurrentHashMap<>();
  private int roomId;

  // 클라이언트가 들어오면 세션 ID와 출력 스트림을 추가
  public void enter(Session session) {
    String sessionId = session.getSessionId();
    OutputStream outputStream = (OutputStream) session.getAttributes().get("ops");

    // participants 맵에 세션 ID와 출력 스트림을 저장
    participants.put(sessionId, outputStream);
    System.out.println("Client entered with ID: " + sessionId);
  }

  public void leave(Session session) {
    participants.remove(session.getSessionId());
    System.out.println("Client left with ID: " + session.getSessionId());
  }

  public void broadcast(Message message) {
    List<Map.Entry<String, OutputStream>> streams = participants.entrySet().stream().toList();
    for (Map.Entry<String, OutputStream> entry : streams) {
      String sessionId = entry.getKey();
      OutputStream outputStream = entry.getValue();
      try {
        outputStream.write((message + "\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
      } catch (SocketException e) {
        participants.remove(sessionId);
        System.out.println("Connection lost with client ID: " + sessionId);
      } catch (IOException e) {
        System.err.println("Error broadcasting message to client ID: " + sessionId);
      }
    }
  }
}
