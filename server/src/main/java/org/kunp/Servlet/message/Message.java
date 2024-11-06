package org.kunp.Servlet.message;

import java.io.Serializable;
import import java.util.HashMap;

public class Message {
  private String type;            // 메시지 타입
  private String sessionId;       // 세션 ID
  private HashMap<String, String> content; // 타입에 따른 유동적인 파라미터

  public Message(String type, String sessionId, HashMap<String, String> content) {
    this.type = type;
    this.sessionId = sessionId;
    this.content = content;
  }

  // 메시지를 파싱하여 Message 객체로 변환하는 정적 메서드
  public static Message parse(String message) {
    String[] parts = message.split("\\|");

    // type과 sessionId를 항상 포함한다고 가정
    String type = parts[0];
    String sessionId = parts[1];

    HashMap<String, String> content = new HashMap<>();

    // 나머지 파라미터는 content로 취급하여 key=value 형식으로 저장
    for (int i = 2; i < parts.length; i++) {
      String[] keyValue = parts[i].split("=");
      if (keyValue.length == 2) {
        content.put(keyValue[0], keyValue[1]);
      }
    }

    return new Message(type, sessionId, content);
  }

  // Getters
  public String getType() {
    return type;
  }

  public String getSessionId() {
    return sessionId;
  }

  public HashMap<String, String> getContent() {
    return content;
  }

  // 디버깅을 위한 toString 메서드
  @Override
  public String toString() {
    return "Message{" +
            "type='" + type + '\'' +
            ", sessionId='" + sessionId + '\'' +
            ", content=" + content +
            '}';
  }
}
