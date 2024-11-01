package org.kunp;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String sessionId;
    private Player player;

    public Client(String TempSessionId) {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            sessionId = TempSessionId;

            // 서버로부터 세션 ID 받음 -> 24.11.01 3시 기준 서버 구현 미완료로 현재 에러 발생
            // sessionId = in.readLine();
            System.out.println("Connected with session ID: " + sessionId);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 플레이어 생성 (임시 사용자)
        player = new Player(250, 250, "술래", "/tagger.png", out, sessionId);

        JFrame frame = new JFrame("Tag Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Map map = new Map(in, out, player, sessionId);
        frame.add(map);
        frame.pack();
        frame.setVisible(true);
    }
}

