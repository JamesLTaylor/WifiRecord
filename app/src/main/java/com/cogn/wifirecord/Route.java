package com.cogn.wifirecord;

import java.util.ArrayList;
import java.util.List;

/**
 * A route through a shopping center
 */
public class Route {
    List<Position> positions;
    public Route(){
        positions = new ArrayList<>();
    }

    public Route copy(){
        Route cloned = new Route();
        for (Position position : positions){
            cloned.addPoint(new Position(position.x, position.y, position.level));
        }
        return cloned;
    }

    public void addPoint(Position position)
    {
        positions.add(position);
    }

    public Position get(int location)
    {
        return positions.get(location);
    }

    public int size() {
        return positions.size();
    }



}
