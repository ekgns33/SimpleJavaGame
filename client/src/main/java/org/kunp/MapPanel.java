package org.kunp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;

public class MapPanel extends JPanel {
    private static final int MAP_SIZE = 500;
    private static final int CELL_SIZE = 10; // 자연스러운 움직임을 위한 셀 사이즈 조정
    private int mapX, mapY;
    private Rectangle[] portals;
    private Image portalImage;
    private Player player;
    private Image image = new ImageIcon(Objects.requireNonNull(getClass().getResource("/tagger.png"))).getImage();
    private static final int IMAGE_SIZE_X = 30;
    private static final int IMAGE_SIZE_Y = 50;
    private HashMap<String, Location> locations = null;

    public MapPanel(int mapX, int mapY, Player player, HashMap<String, Location> locations) {
        this.mapX = mapX;
        this.mapY = mapY;
        this.player = player;
        this.locations = locations;
        setPreferredSize(new Dimension(MAP_SIZE, MAP_SIZE));
        setFocusable(true);
        initializePortals();
        portalImage = new ImageIcon(Objects.requireNonNull(getClass().getResource("/portal.png"))).getImage();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        player.move(0, -CELL_SIZE);
                        break;
                    case KeyEvent.VK_DOWN:
                        player.move(0, CELL_SIZE);
                        break;
                    case KeyEvent.VK_LEFT:
                        player.move(-CELL_SIZE, 0);
                        break;
                    case KeyEvent.VK_RIGHT:
                        player.move(CELL_SIZE, 0);
                        break;
                }
                //repaint();
            }
        });
    }

    private void initializePortals() {
        portals = new Rectangle[4];
        int portalSize = CELL_SIZE * 5;
        portals[0] = new Rectangle(MAP_SIZE / 2 - portalSize / 2, 0, portalSize, portalSize); // 북쪽 포탈
        portals[1] = new Rectangle(MAP_SIZE / 2 - portalSize / 2, MAP_SIZE - portalSize, portalSize, portalSize); // 남쪽 포탈
        portals[2] = new Rectangle(0, MAP_SIZE / 2 - portalSize / 2, portalSize, portalSize); // 서쪽 포탈
        portals[3] = new Rectangle(MAP_SIZE - portalSize, MAP_SIZE / 2 - portalSize / 2, portalSize, portalSize); // 동쪽 포탈

        // 가장자리 맵의 경우 접근 가능한 쪽에만 포탈 배치
        if (mapX == 0) portals[2] = null; // 서쪽 포탈 제거
        if (mapX == 2) portals[3] = null; // 동쪽 포탈 제거
        if (mapY == 0) portals[0] = null; // 북쪽 포탈 제거
        if (mapY == 2) portals[1] = null; // 남쪽 포탈 제거
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawMap(g);
        drawPlayer(g);
        drawCoMapUsers(g);
    }

    private void drawMap(Graphics g) {
        g.setColor(new Color(200, 200, 200));
        for (int i = 0; i < MAP_SIZE; i += CELL_SIZE) {
            for (int j = 0; j < MAP_SIZE; j += CELL_SIZE) {
                g.drawRect(i, j, CELL_SIZE, CELL_SIZE);
            }
        }

        for (Rectangle portal : portals) {
            if (portal != null) {
                g.drawImage(portalImage, portal.x, portal.y, portal.width, portal.height, null);
            }
        }
    }

    private void drawPlayer(Graphics g) {
        player.draw(g);
    }

    private void drawCoMapUsers(Graphics g) {
        synchronized (locations) {
            Collection collection = locations.values();
            Iterator iter = collection.iterator();
            while(iter.hasNext()){
                Location loc = (Location) iter.next();
                if(loc.getRoomNumber() == mapY * 3 + mapX + 1){
                    g.drawImage(image, loc.getX(), loc.getY(), IMAGE_SIZE_X, IMAGE_SIZE_Y, null);
                }
            }
        }
    }

    public int isPortal(int x, int y) {
        Rectangle portalRange = new Rectangle(x, y, 1, IMAGE_SIZE_Y);
        for (int i = 0; i < portals.length; i++) {
            if (portals[i] != null && portals[i].intersects(portalRange)) {
                return i;
            }
        }
        return -1;
    }
}
