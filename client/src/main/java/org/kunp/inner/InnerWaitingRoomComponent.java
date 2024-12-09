package org.kunp.inner;

import org.kunp.manager.ScreenManager;
import org.kunp.manager.ServerCommunicator;
import org.kunp.manager.StateManager;
import org.kunp.map.MapComponent;
import org.kunp.map.PlayerComponent;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class InnerWaitingRoomComponent extends JPanel {
  public InnerWaitingRoomComponent(StateManager stateManager, ServerCommunicator serverCommunicator, ScreenManager screenManager, String roomName, BufferedReader in, PrintWriter out) {
    this(stateManager, serverCommunicator, screenManager, roomName, in, out, new CopyOnWriteArraySet<>());
  }

  public InnerWaitingRoomComponent(StateManager stateManager, ServerCommunicator serverCommunicator, ScreenManager screenManager,
                                   String roomName, BufferedReader in, PrintWriter out, CopyOnWriteArraySet<String> existingSessionIds) {
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createLineBorder(Color.GRAY));
    setPreferredSize(new Dimension(350, 200));

    JLabel nameLabel = new JLabel("게임 룸");
    nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
    add(nameLabel, BorderLayout.NORTH);

    InnerWaitingRoomListPanel listPanel = new InnerWaitingRoomListPanel(existingSessionIds);
    add(listPanel, BorderLayout.CENTER);

    InnerWaitingRoomControlPanel controlPanel = new InnerWaitingRoomControlPanel(stateManager, roomName);
    add(controlPanel, BorderLayout.SOUTH);

    ServerCommunicator.ServerMessageListener listener = message -> 
        handleServerMessage(message, existingSessionIds, listPanel, in, out, stateManager, serverCommunicator, screenManager, roomName);

    stateManager.addMessageListener(listener);
  }

  private void handleServerMessage(String message, CopyOnWriteArraySet<String> sessionIds, InnerWaitingRoomListPanel listPanel, BufferedReader in, PrintWriter out, StateManager stateManager,  ServerCommunicator serverCommunicator, ScreenManager screenManager, String roomName) {
    String[] tokens = message.split("\\|");
    if (tokens.length > 1) {
      String type = tokens[0];
        switch (type) {
            case "110" -> {
                String[] sessions = message.split("\n");
                String lastSessionMessage = sessions[sessions.length - 1];
                String[] sessionData = lastSessionMessage.split("\\|");
                if (sessionData.length > 1) {
                    String[] sessionIdsArray = sessionData[1].split(",");
                    Set<String> newSessionIds = Set.of(sessionIdsArray);
                    sessionIds.addAll(newSessionIds);
                }
            }
            case "111" -> {
                String[] data = tokens[1].split(",");
                sessionIds.remove(data[0]);
            }
            case "113" -> SwingUtilities.invokeLater(() -> {
                System.out.println("Game starting...");
                try {
                    int gameId = Integer.parseInt(tokens[1]);
                    String role = tokens[2].equals("0") ? "tagger" : "normal";
                    PlayerComponent player = new PlayerComponent(stateManager, serverCommunicator, screenManager,
                            Integer.parseInt(tokens[3]), Integer.parseInt(tokens[4]), role, out, stateManager.getSessionId(), gameId
                    );
                    screenManager.addScreen("Map", new MapComponent(stateManager, serverCommunicator, screenManager, player, stateManager.getSessionId(), in, out, gameId, roomName, sessionIds));
                    stateManager.switchTo("Map");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
      SwingUtilities.invokeLater(() -> listPanel.updateSessionList(sessionIds));
    }
  }
}