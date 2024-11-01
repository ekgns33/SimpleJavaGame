package org.kunp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class Map extends JFrame {
    private static final int VIEW_SIZE = 500;
    private static final int MOVE_STEP = 10;
    private static final int PLAYER_X = 250;
    private static final int PLAYER_Y = 250;
    private MapPanel[][] maps;
    private int currentMapX = 1, currentMapY = 1;
    private List<Player> players;
    private boolean[] keysPressed = new boolean[256]; // 키 상태 추적용 배열
    private Timer moveTimer;

    public Map() {
        setTitle("Tag Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        players = new ArrayList<>();

        // 테스트를 위한 술래, 도둑 플레이어 추가
        players.add(new Player(VIEW_SIZE / 2, VIEW_SIZE / 2, "술래", "/tagger.png"));
        players.add(new Player(VIEW_SIZE / 2 + 50, VIEW_SIZE / 2 + 50, "도둑", "/normal.png"));

        maps = new MapPanel[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                maps[i][j] = new MapPanel(i, j, players);
            }
        }
        setLayout(new BorderLayout());
        add(maps[currentMapX][currentMapY], BorderLayout.CENTER);

        /* 맵 크기와 화면 크기가 같도록 설정 */
        setPreferredSize(new Dimension(VIEW_SIZE, VIEW_SIZE));
        pack();
        Insets insets = getInsets();
        setSize(VIEW_SIZE + insets.left + insets.right, VIEW_SIZE + insets.top + insets.bottom);

        /* 방향키를 눌렀을 때 이동에 지연어 없도록 하기위함 */
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                keysPressed[e.getKeyCode()] = true; // 키가 눌렸음을 저장
            }
            @Override
            public void keyReleased(KeyEvent e) {
                keysPressed[e.getKeyCode()] = false; // 키가 떼어졌음을 저장
            }
        });
        // Timer 설정: 50ms마다 유저의 키 상태를 체크
        moveTimer = new Timer(50, e -> checkMovement());
        moveTimer.start();

        setFocusable(true);
        requestFocusInWindow();
        setVisible(true);
    }

    private void checkMovement() {
        if (keysPressed[KeyEvent.VK_UP]) movePlayer(KeyEvent.VK_UP);
        if (keysPressed[KeyEvent.VK_DOWN]) movePlayer(KeyEvent.VK_DOWN);
        if (keysPressed[KeyEvent.VK_LEFT]) movePlayer(KeyEvent.VK_LEFT);
        if (keysPressed[KeyEvent.VK_RIGHT]) movePlayer(KeyEvent.VK_RIGHT);
    }

    private void movePlayer(int keyCode) {
        Player player = players.get(0);
        int newX = player.getX(), newY = player.getY();
        switch (keyCode) {
            case KeyEvent.VK_UP:
                newY = Math.max(player.getY() - MOVE_STEP, 0);
                break;
            case KeyEvent.VK_DOWN:
                newY = Math.min(player.getY() + MOVE_STEP, VIEW_SIZE - player.getImageSizeY());
                break;
            case KeyEvent.VK_LEFT:
                newX = Math.max(player.getX() - MOVE_STEP, 0);
                break;
            case KeyEvent.VK_RIGHT:
                newX = Math.min(player.getX() + MOVE_STEP, VIEW_SIZE - player.getImageSizeX());
                break;
        }
        int portal = -1;
        if ((portal = maps[currentMapX][currentMapY].isPortal(newX, newY)) != -1) {
            moveMap(newX, newY, portal);
        } else {
            player.move(newX - player.getX(), newY - player.getY());
            repaint();
        }
        updateLocationLabel();
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
        for (Player player : players) {
            player.move(VIEW_SIZE / 2 - player.getX(), VIEW_SIZE / 2 - player.getY());
        }
        add(maps[currentMapX][currentMapY], BorderLayout.CENTER);
        revalidate();
        repaint();
        requestFocusInWindow();
        updateLocationLabel();
    }

    private void updateLocationLabel() {
        Player player = players.get(0); // 현재는 첫 번째 플레이어만 출력
        System.out.println("현재 맵: (" + currentMapX + ", " + currentMapY + ") | 플레이어 위치: (" + player.getX() + ", " + player.getY() + ")");
    }
}
