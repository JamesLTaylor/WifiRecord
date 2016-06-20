package com.cogn.wifirecord;

import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Graph {

    private SparseArray<Graph.Node> nodes;
    private double pxPerM;

    public Graph(){}


    /**
     * Load from a JSON file.
     * File must be a map of index - x
     *                             - y
     *                             - level
     *                             - [nodeTo1, nodeTo2, ...]
     */
    public void loadFromFile(InputStream inputStream, double pxPerM){
        this.pxPerM = pxPerM;
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder jsonString = new StringBuilder();
        JSONObject jsonReader;
        try {
            String str;
            while ((str = in.readLine()) != null) {
                jsonString.append(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            jsonReader = new JSONObject(jsonString.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        nodes = new SparseArray<>();
        Iterator<String> keyIter = jsonReader.keys();
        while (keyIter.hasNext())
        {
            String key = keyIter.next();
            int keyInt = Integer.parseInt(key);

            try {
                JSONObject jsonObject = jsonReader.getJSONObject(key);
                float x = (float)jsonObject.getDouble("x");
                float y = (float)jsonObject.getDouble("y");
                int level = jsonObject.getInt("level");

                JSONArray nodesTo = jsonObject.getJSONArray("to");
                List<Integer> connected = new ArrayList<>();
                for(int i=0; i < nodesTo.length(); i++){
                    connected.add(nodesTo.getInt(i));
                }
                nodes.put(keyInt, new Graph.Node(new Position(x, y, level), connected));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


    }

    /**
     * Finds the shortest distance on the graph between two points.
     *
     */
    public Route getRoute(Position start, Position end)
    {
        double minDStart = 1e9;
        double minDEnd = 1e9;

        int startInd = 0;
        int endInd = 0;

        for (int i = 0; i < nodes.size(); i++) {
            nodes.valueAt(i).dist = 1e9f;
            nodes.valueAt(i).route = new Route(pxPerM);
            double ds = start.getDistanceTo(nodes.valueAt(i).pos);
            double de = end.getDistanceTo(nodes.valueAt(i).pos);
            if (ds<minDStart) {
                minDStart = ds;
                startInd = nodes.keyAt(i);
            }
            if (de<minDEnd) {
                minDEnd = de;
                endInd = nodes.keyAt(i);
            }
        }
        nodes.get(startInd).dist = 0;
        nodes.get(startInd).route.addPoint(nodes.get(startInd).pos);

        HashSet<Integer> vertexSet = new HashSet<>();
        for (int i = 0; i<nodes.size(); i++){
            vertexSet.add(nodes.keyAt(i));
        }

        int current = startInd;
        while (vertexSet.size()>0) {
            double minDist = 1e9;
            // Find the lowest dist node
            for (Integer i : vertexSet) {
                if (nodes.get(i).dist < minDist) {
                    current = i;
                    minDist = nodes.get(i).dist;
                }
            }
            if (current == endInd) {
                break;
            }

            Node nodeA = nodes.get(current);
            for (int to : nodes.get(current).connected) {
                Node nodeB = nodes.get(to);
                double d = nodeA.dist + nodeA.pos.getDistanceTo(nodeB.pos);
                if (d<nodeB.dist) {
                    nodeB.dist = d;
                    Route newRoute = nodeA.route.copy();
                    newRoute.addPoint(nodes.get(to).pos);
                    nodeB.route = newRoute;
                }
            }
            vertexSet.remove(current);
        }

        nodes.get(endInd).route.finalizeConstruction();
        return nodes.get(endInd).route;
    }


    public Route getRoute(Position start, Shop endShop)
    {
        double minD = 1e9;

        Route route = null;
        for (Position entranceLocation : endShop.entranceLocations)
        {
            Route tempRoute = getRoute(start, entranceLocation);
            if (tempRoute.getPathLength()<minD) {
                route = tempRoute;
            }
        }
        route.createDescription();
        return route;
    }


    public class Node {
        public Position pos;
        public List<Integer> connected;
        public double dist;
        public Route route;
        public Node(Position pos, List<Integer> connected)
        {
            this.connected = connected;
            this.pos = pos;
        }
    }
}
