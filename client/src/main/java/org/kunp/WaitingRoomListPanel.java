package org.kunp;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

// 대기실 목록 리스트 패널
public class WaitingRoomListPanel extends JPanel {
    public WaitingRoomListPanel(BufferedReader in, PrintWriter out, String sessionId, JPanel parentPanel) {
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("대기실 목록"));

        // 대기실 목록은 스크롤이 가능한 리스트
        JPanel roomListPanel = new JPanel();
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(roomListPanel);
        scrollPane.setPreferredSize(new Dimension(400, 250));

        // 마우스 휠 스크롤시 속도 설정
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        //임시로 대기실 인원 명단 8인 추가
        List<String> sessionIds = new ArrayList<>();
        sessionIds.add("지원");
        sessionIds.add("경식");
        sessionIds.add("다훈");
        sessionIds.add("세훈");
        sessionIds.add("양찬");
        sessionIds.add("네트워크");
        sessionIds.add("프로그래밍");
        sessionIds.add("12팀");

        // 임시로 대기실 컴포넌트 20개 추가
        List<WaitingRoomComponent> rooms = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            rooms.add(new WaitingRoomComponent(in, out, sessionId, "대기실 " + i, parentPanel, sessionIds));
        }
        for (WaitingRoomComponent room : rooms) {
            roomListPanel.add(room);
        }
        add(scrollPane, BorderLayout.CENTER);
    }
}