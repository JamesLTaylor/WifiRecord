package com.cogn.wifirecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 6/16/2016.
 */
public class Shop {
    String name;
    List<Position> entranceLocations;
    List<String> entranceNames;

    public List<Position> getEntranceLocations() {
        return entranceLocations;
    }

    public List<String> getEntranceNames() {
        return entranceNames;
    }

    public Shop(){
        entranceLocations = new ArrayList<>();
        entranceNames = new ArrayList<>();
    }


    @Override
    public boolean equals(Object o) {
        if (o.getClass() == this.getClass()) {
            return this.name.equals( ((Shop)o).getName());
        }
        else {
            return super.equals(o);
        }
    }

    /**
     *
     * @param name name will be overwritten, only call this method if you know it is the same shop as already exists/
     */
    public void add(String name, int level, float x, float y, String entranceName)
    {
        this.name = name;
        this.entranceLocations.add(new Position(x, y, level));
        entranceNames.add(entranceName);
    }


    public String getName() {
        return name;
    }
}
