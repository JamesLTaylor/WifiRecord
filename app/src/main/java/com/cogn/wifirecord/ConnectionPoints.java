package com.cogn.wifirecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/***
 * Class to store the locations of connection points between the levels.
 */
public class ConnectionPoints{
    private List<List<List<Float>>> connectionPoints;
    public ConnectionPoints(){
        connectionPoints = new ArrayList<>();
    }
    public void add(int level0, float x0, float y0, int level1, float x1, float y1){
        List<Float> point0 = Arrays.asList(x0, y0);
        List<Float> point1 = Arrays.asList(x1, y1);
        List<List<Float>> pair = Arrays.asList(point0, point1);
        connectionPoints.add(pair);
    }

    public int IndexOfClosest(int level, float refX, float refY) {
        double dBest = 1e9;
        int indBest = -1;
        for (int i = 0; i<connectionPoints.size(); i++){
            float dx = connectionPoints.get(i).get(level).get(0) - refX;
            float dy = connectionPoints.get(i).get(level).get(1) - refY;
            double d = Math.sqrt(dx*dx + dy*dy);
            if (d<dBest) {
                dBest = d;
                indBest = i;
            }
        }
        return indBest;
    }

    public float getX(int index, int level){
        return connectionPoints.get(index).get(level).get(0);
    }

    public float getY(int index, int level){
        return connectionPoints.get(index).get(level).get(1);
    }
}