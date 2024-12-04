package org.kunp.map;

import org.kunp.ScreenManager;
import org.kunp.ServerCommunicator;
import org.kunp.StateManager;

import java.io.PrintWriter;

public class Player {
    private int x, y;
    private String role;
    private String sessionId;
    private int mapIdx;
    private PrintWriter out;
    private ServerCommunicator serverCommunicator;

    public Player(StateManager stateManager, ServerCommunicator serverCommunicator, ScreenManager screenManager, int startX, int startY, String role, PrintWriter out, String sessionId) {
        this.x = startX;
        this.y = startY;
        this.role = role;
        this.out = out;
        this.sessionId = sessionId;
        this.mapIdx = 5;
        this.serverCommunicator = serverCommunicator;
    }

    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public String getRole() {
        return role;
    }
    public String getSessionId() {
        return sessionId;
    }
    public int getMapIdx() {
        return mapIdx;
    }
    public void setMapIdx(int mapIdx) {
        this.mapIdx = mapIdx;
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
        sendLocation();
    }

    public void sendInteraction(){
        String requestMessage = String.format("202|%s|%s|%d|%d", sessionId, x, y, mapIdx);
        serverCommunicator.sendRequest(requestMessage);
    }

    private void sendLocation() {
        String requestMessage = String.format("201|%s|%s|%d|%d", sessionId, x, y, mapIdx);
        serverCommunicator.sendRequest(requestMessage);
    }
}

