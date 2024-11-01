package org.kunp;

import javax.swing.*;
import java.awt.*;

public class Player {
    private int x, y;
    private Image image;
    private String role;
    private static final int IMAGE_SIZE_X = 30;
    private static final int IMAGE_SIZE_Y = 50;

    public Player(int startX, int startY, String role, String imagePath) {
        this.x = startX;
        this.y = startY;
        this.role = role;
        this.image = new ImageIcon(getClass().getResource(imagePath)).getImage();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getImageSizeX(){
        return IMAGE_SIZE_X;
    }

    public int getImageSizeY(){
        return IMAGE_SIZE_Y;
    }

    //todo: 술래, 도둑 랜덤 설정
    public String getRole() {
        return role;
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public void draw(Graphics g) {
        g.drawImage(image, x, y, IMAGE_SIZE_X, IMAGE_SIZE_Y, null);
    }
}


