package org.kunp.inner;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class InnerWaitingRoomListPanel extends JPanel {
    private final JPanel gridPanel;

    public InnerWaitingRoomListPanel(Set<String> sessionIds) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        setPreferredSize(new Dimension(350, 150));

        gridPanel = new JPanel();
        gridPanel.setLayout(new GridLayout(0, 2, 10, 10)); // 2열 그리드 레이아웃
        add(new JScrollPane(gridPanel), BorderLayout.CENTER);

        updateSessionList(sessionIds);
    }

    public void updateSessionList(Set<String> sessionIds) {
        gridPanel.removeAll();
        synchronized (sessionIds) {
            for (String id : sessionIds) {
                JPanel userPanel = new JPanel();
                userPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                userPanel.setLayout(new GridBagLayout());

                JLabel label = new JLabel(id);
                label.setFont(new Font("Arial", Font.BOLD, 16));
                label.setHorizontalAlignment(SwingConstants.CENTER);

                userPanel.add(label);
                gridPanel.add(userPanel);
            }
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }
}
