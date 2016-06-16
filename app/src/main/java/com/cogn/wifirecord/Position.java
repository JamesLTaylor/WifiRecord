package com.cogn.wifirecord;


public class Position {
    public float x;
    public float y;
    public int level;
    public Position(float x, float y, int level){
        this.level = level;
        this.x = x;
        this.y = y;
    }

    public double getDistanceTo(Position pos) {
        double dist = Math.sqrt((x-pos.x)*(x-pos.x) + (y-pos.y)*(y-pos.y)) + 100.0 * Math.abs(level-pos.level);
        return dist;
    }
}
