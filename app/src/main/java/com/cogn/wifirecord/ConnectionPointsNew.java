package com.cogn.wifirecord;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by James on 7/28/2016.
 */
public class ConnectionPointsNew {
    Map<Integer, List<List<Position>>> connections;

    public ConnectionPointsNew(){
        connections = new HashMap<>();
    }

    /**
     * User must add connections in both directions
     *
     * @param start starting position of connection
     * @param end ending positing of connection
     */
    public void addConnection(Position start, Position end){
        List<Position> thisConnection = new ArrayList<>();
        thisConnection.add(start);
        thisConnection.add(end);
        List<List<Position>> thisList;
        if (connections.containsKey(start.level)) {
            thisList = connections.get(start.level);
        } else {
            thisList = new ArrayList<>();
            connections.put(start.level, thisList);
        }
        thisList.add(thisConnection);
    }

    /**
     *
     * @return returns the connection point on this level that is closest and where it connects to.
     */
    public List<Position> getNearestConnectionPoint(Position position, int toLevel) {
        if (!connections.containsKey(position.level))
            throw new IllegalArgumentException("There are no connections from level " + position.level );

        List<List<Position>> pointPairs = connections.get(position.level);
        List<Position> bestPosition = null;
        Double minD = 1e9;
        for (List<Position> pointPair : pointPairs) {
            if (pointPair.get(1).level==toLevel) {
                double distance = pointPair.get(0).getDistanceTo(position);
                if (distance < minD) {
                    bestPosition = pointPair;
                    minD = distance;
                }
            }
        }
        if (bestPosition==null)
            throw new IllegalArgumentException("There are no direct connections from level " + position.level + "to level " + toLevel);
        return bestPosition;
    }
}
