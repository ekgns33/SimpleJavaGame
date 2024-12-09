package org.kunp.map;

import org.kunp.manager.ScreenManager;
import org.kunp.manager.ServerCommunicator;
import org.kunp.manager.StateManager;
import org.kunp.result.ResultComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class MapComponent extends JPanel {
    private MapComponentPanel[][] maps;
    private int currentMapX = 1, currentMapY = 1;
    private final PlayerComponent player;
    private final boolean[] keysPressed = new boolean[256];
    private final Timer moveTimer;
    private final String sessionId;
    private final HashMap<String, Location> locations = new HashMap<>();
    private final int gameId;

    public MapComponent(StateManager stateManager, ServerCommunicator serverCommunicator, ScreenManager screenManager, PlayerComponent player, String sessionId, BufferedReader in, PrintWriter out, int gameId, String roomName, CopyOnWriteArraySet<String> sessionIds) {
        this.player = player;
        this.sessionId = sessionId;
        this.gameId = gameId;

        setPreferredSize(new Dimension(Constants.MAP_SIZE, Constants.MAP_SIZE));
        setFocusable(true);
        initializeMapPanels();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                keysPressed[e.getKeyCode()] = true;
            }
            @Override
            public void keyReleased(KeyEvent e) {
                keysPressed[e.getKeyCode()] = false;
            }
        });

        addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                for (int i = 0; i < keysPressed.length; i++) {
                    keysPressed[i] = false;
                }
            }
            @Override
            public void focusGained(FocusEvent e) {
            }
        });

        moveTimer = new Timer(50, e -> checkKeyboardInput());
        moveTimer.start();

        serverCommunicator.addMessageListener(message -> {
            String[] tokens = message.split("\\|");
            String type = tokens[0];
            if(type.equals("210")){
                String mover_sessionId = tokens[1];
                int x = Integer.parseInt(tokens[2]), y = Integer.parseInt(tokens[3]);
                int mapIdx = Integer.parseInt(tokens[4]);
                int role = Integer.parseInt(tokens[6]);
                synchronized (locations){
                    if(!locations.containsKey(mover_sessionId)) locations.put(mover_sessionId, new Location(role, mapIdx, x, y));
                    else locations.get(mover_sessionId).setLocation(role, mapIdx, x, y);
                }
                repaint();
            }
            else if(type.equals("211") || type.equals("212")){
                String target_sessionId = tokens[1];
                int x = Integer.parseInt(tokens[2]), y = Integer.parseInt(tokens[3]);
                int mapIdx = Integer.parseInt(tokens[4]);
                int role = Integer.parseInt(tokens[6]);
                synchronized (locations){
                    if(!locations.containsKey(target_sessionId)) locations.put(target_sessionId, new Location(role, mapIdx, x, y));
                    else locations.get(target_sessionId).setLocation(role, mapIdx, x, y);
                }
                if(target_sessionId.equals(this.sessionId)){
                    setView(mapIdx, x, y);
                }
                repaint();
            }else if(type.equals("213")){
                String result = Integer.parseInt(tokens[2]) == 1 ? "도망자 승리" : "술래 승리";
                screenManager.addScreen("Result", new ResultComponent(stateManager, serverCommunicator, screenManager, result, roomName, in, out, sessionIds));
                SwingUtilities.invokeLater(() -> {
                    stateManager.switchTo("Result");
                });
            }else if(type.equals("214")){
                String disconnectedSessionId = tokens[1];
                sessionIds.remove(disconnectedSessionId);
                synchronized (locations){
                    locations.remove(disconnectedSessionId);
                }
            }
        });

        // 포커스 요청
        SwingUtilities.invokeLater(() -> {
            if (!requestFocusInWindow()) {
                Timer focusRetryTimer = new Timer(100, evt -> {
                    if (isFocusOwner() || requestFocusInWindow()) {
                        ((Timer) evt.getSource()).stop(); // 성공하면 타이머 중지
                        System.out.println("Focus successfully set on Map.");
                    }
                });
                focusRetryTimer.start();
            } else {
                System.out.println("Focus successfully set on Map.");
            }
        });
    }

    private void initializeMapPanels() {
        maps = new MapComponentPanel[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                maps[i][j] = new MapComponentPanel(i, j, locations);
            }
        }

        if(player.getRole().equals("tagger")){
            currentMapX = 2;
            currentMapY = 0;
            player.setMapIdx(3);
        }else{
            currentMapX = 0;
            currentMapY = 2;
            player.setMapIdx(7);
        }
        player.move(Constants.MAP_SIZE / 2 - player.getX(), Constants.MAP_SIZE / 2 - player.getY());
        setLayout(new BorderLayout());
        add(maps[currentMapX][currentMapY], BorderLayout.CENTER);
    }

    private void checkKeyboardInput() {
        if (keysPressed[KeyEvent.VK_UP]) movePlayer(KeyEvent.VK_UP);
        if (keysPressed[KeyEvent.VK_DOWN]) movePlayer(KeyEvent.VK_DOWN);
        if (keysPressed[KeyEvent.VK_LEFT]) movePlayer(KeyEvent.VK_LEFT);
        if (keysPressed[KeyEvent.VK_RIGHT]) movePlayer(KeyEvent.VK_RIGHT);

        if (keysPressed[KeyEvent.VK_E] || keysPressed[KeyEvent.VK_SPACE]) {
            player.sendInteraction();
        }
    }

    private void movePlayer(int keyCode) {
        int newX = player.getX(), newY = player.getY();
        int xx = 0, yy = 0;
        switch (keyCode) {
            case KeyEvent.VK_UP:
                yy = Math.max(player.getY() - Constants.CELL_SIZE, 0);
                if(maps[currentMapX][currentMapY].movable(newX, yy)) newY = yy;
                break;
            case KeyEvent.VK_DOWN:
                yy = Math.min(player.getY() + Constants.CELL_SIZE, Constants.MAP_SIZE - Constants.PLAYER_SIZE_Y);
                if(maps[currentMapX][currentMapY].movable(newX, yy)) newY = yy;
                break;
            case KeyEvent.VK_LEFT:
                xx = Math.max(player.getX() - Constants.CELL_SIZE, 0);
                if(maps[currentMapX][currentMapY].movable(xx, newY)) newX = xx;
                break;
            case KeyEvent.VK_RIGHT:
                xx = Math.min(player.getX() + Constants.CELL_SIZE, Constants.MAP_SIZE - Constants.PLAYER_SIZE_X);
                if(maps[currentMapX][currentMapY].movable(xx, newY)) newX = xx;
                break;
        }

        int portal;
        if ((portal = maps[currentMapX][currentMapY].isPortal(newX, newY)) != -1) {
            moveMap(newX, newY, portal);
        } else {
            player.move(newX - player.getX(), newY - player.getY());
        }
    }

    private void moveMap(int x, int y, int portal) {
        int newX = currentMapX, newY = currentMapY;
        if (portal == 2) newX = Math.max(currentMapX - 1, 0); // 서쪽 포탈
        if (portal == 3) newX = Math.min(currentMapX + 1, 2); // 동쪽 포탈
        if (portal == 0) newY = Math.max(currentMapY - 1, 0); // 북쪽 포탈
        if (portal == 1) newY = Math.min(currentMapY + 1, 2); // 남쪽 포탈

        remove(maps[currentMapX][currentMapY]);
        currentMapX = newX;
        currentMapY = newY;
        player.setMapIdx(newY * 3 + newX + 1);

        int portalSize = Constants.CELL_SIZE * 5;
        if (portal == 2) player.move(Constants.MAP_SIZE - 2*portalSize - Constants.PLAYER_SIZE_X - 10, 0);
        if (portal == 3) player.move(-Constants.MAP_SIZE + 2*portalSize + 10, 0);
        if (portal == 0) player.move(0, Constants.MAP_SIZE - 2*portalSize - Constants.PLAYER_SIZE_Y-20);
        if (portal == 1) player.move(0, -Constants.MAP_SIZE + 2*portalSize + Constants.PLAYER_SIZE_Y+20);
        add(maps[currentMapX][currentMapY], BorderLayout.CENTER);
        revalidate();
        requestFocusInWindow();
    }

    private void setView(int mapIdx, int x, int y){
        remove(maps[currentMapX][currentMapY]);
        currentMapX = 0;
        currentMapY = 0;
        player.setMapIdx(mapIdx);
        player.setLoc(x, y);
        add(maps[currentMapX][currentMapY], BorderLayout.CENTER);
        revalidate();
        requestFocusInWindow();
    }
}
