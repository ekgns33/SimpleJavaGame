package org.kunp;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.util.Objects;

public class Player {
    private int x, y;
    private Image image;
    private String role;
    private String sessionId;
    private int roomNumber;
    private PrintWriter out;
    private static final int IMAGE_SIZE_X = 30;
    private static final int IMAGE_SIZE_Y = 50;

    public Player(int startX, int startY, String role, String imagePath, PrintWriter out, String sessionId) {
        this.x = startX;
        this.y = startY;
        this.role = role;
        this.image = new ImageIcon(Objects.requireNonNull(getClass().getResource(imagePath))).getImage();
        this.out = out;
        this.sessionId = sessionId;
        this.roomNumber = 5; // 초기 roomNumber 설정 (중앙 맵)
    }

    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public int getImageSizeX() {
        return IMAGE_SIZE_X;
    }
    public int getImageSizeY() {
        return IMAGE_SIZE_Y;
    }
    public String getRole() {
        return role;
    }
    public String getSessionId() {
        return sessionId;
    }
    public int getRoomNumber() {
        return roomNumber;
    }
    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
        sendLocation();
    }

    private void sendLocation() {
        String message = String.format("1|%s|%d|%d|%d", sessionId, x, y, roomNumber);
        out.println(message);
        out.flush();
    }

    public void draw(Graphics g) {
        g.drawImage(image, x, y, IMAGE_SIZE_X, IMAGE_SIZE_Y, null);
    }
}

