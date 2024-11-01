package org.kunp;

public class Location{
    int roomNumber, x, y;
    public Location(int roomNumber, int x, int y){
        this.roomNumber = roomNumber;
        this.x = x;
        this.y = y;
    }
    public void setLocation(int roomNumber, int x, int y){
        this.roomNumber = roomNumber;
        this.x = x;
        this.y = y;
    }
    public int getRoomNumber(){
        return roomNumber;
    }
    public int getX(){
        return x;
    }
    public int getY(){
        return y;
    }
}