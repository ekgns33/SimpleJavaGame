package org.kunp.result;

import org.kunp.manager.ScreenManager;
import org.kunp.manager.ServerCommunicator;
import org.kunp.manager.StateManager;
import org.kunp.inner.InnerWaitingRoomComponent;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.concurrent.CopyOnWriteArraySet;

public class ResultComponent extends JPanel {
    public ResultComponent(StateManager stateManager, ServerCommunicator serverCommunicator, 
                         ScreenManager screenManager, String gameResult, String roomName, BufferedReader in, PrintWriter out, CopyOnWriteArraySet<String> sessionIds) {

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(500, 500));

        // 결과 패널 생성
        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 게임 결과 레이블
        JLabel resultLabel = new JLabel(gameResult);
        resultLabel.setFont(new Font("Arial", Font.BOLD, 24));
        resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 대기실 돌아가기 버튼
        JButton returnButton = new JButton("대기실로 돌아가기");
        returnButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 컴포넌트 배치
        resultPanel.add(Box.createVerticalGlue());
        resultPanel.add(resultLabel);
        resultPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        resultPanel.add(returnButton);
        resultPanel.add(Box.createVerticalGlue());
        add(resultPanel, BorderLayout.CENTER);

        returnButton.addActionListener(e -> {
            InnerWaitingRoomComponent innerWaitingRoom = new InnerWaitingRoomComponent(
                stateManager, serverCommunicator, screenManager, roomName, in, out, sessionIds);
            screenManager.addScreen("InnerWaitingRoom", innerWaitingRoom);
            stateManager.switchTo("InnerWaitingRoom");
        });
    }
}
