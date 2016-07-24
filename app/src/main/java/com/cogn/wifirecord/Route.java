package com.cogn.wifirecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A route through a shopping center
 */
public class Route {
    List<Position> pathPoints;
    List<Double> pathFractions;

    double pathLength;
    double pxPerM;

    public ArrayList<Description> descriptions;

    public Route(double pxPerM){
        this.pxPerM = pxPerM;
        pathPoints = new ArrayList<>();
    }

    public double getPathLength() {
        return pathLength;
    }


    public void addPoint(Position position){
        pathPoints.add(position);
    }

    public Route copy(){
        Route cloned = new Route(pxPerM);
        for (Position position : pathPoints){
            cloned.addPoint(new Position(position.x, position.y, position.level));
        }
        cloned.finalizeConstruction();
        return cloned;
    }

    public void finalizeConstruction(){
        pathFractions = new ArrayList<>(pathPoints.size());
        pathFractions.add(0, 0.0);
        for (int i = 0; i < pathPoints.size()-1; i++) {
            double dx = pathPoints.get(i+1).x - pathPoints.get(i).x;
            double dy = pathPoints.get(i+1).y - pathPoints.get(i).y;
            double frac = pathFractions.get(i) + Math.sqrt(dx*dx + dy*dy);
            pathFractions.add(i+1, frac);
        }
        pathLength = pathFractions.get(pathFractions.size()-1);
        for (int i = 0; i < pathFractions.size(); i++) {
            pathFractions.set(i, pathFractions.get(i)/pathLength);
        }
    }

    public void createDescription()
    {
        descriptions = new ArrayList<>();

        double separationM = 5;
        double separationPx = separationM*pxPerM;
        List<Shop> shops = GlobalDataFragment.shopDirectory.getAllShops();
        for (Shop shop : shops) {
            for (int i = 0; i < shop.entranceLocations.size(); i++) {
                Double[] dFracXAndY = closest(shop.entranceLocations.get(i));
                if (dFracXAndY[0] <= separationPx) {
                    double d = dFracXAndY[0];
                    double frac= dFracXAndY[1];
                    float x = dFracXAndY[2].floatValue();
                    float y = dFracXAndY[3].floatValue();
                    descriptions.add(new Description(shop, i, x, y, d, frac));
                }
            }
        }
        Collections.sort(descriptions);

//            route_shops = [descriptions[0]]
//            separation = shop_separation_px / path.len
//            for i in range(1,len(descriptions)):
//            if (descriptions[i][2]- route_shops[-1][2])>separation:
//            route_shops.append(descriptions[i])
//
//            if not route_shops[-1][0]==descriptions[-1][0]:
//            #route_shops.pop()
//            route_shops.append(descriptions[-1])

    }


    /**
     *
     * @param referencePoint the point being compared to the path
     * @return a vector with elements:
     *              0 - minimum distance to path
     *              1 - the fraction along the path at which the closest point
     *              2 - the closest point, x value
     *              3 - the closest point, y value
     */
    private Double[] closest(Position referencePoint)
    {
        Double minD = 1e9;
        Double closestX = (double) pathPoints.get(0).x;
        Double closestY = (double) pathPoints.get(0).y;
        for (int i = 0; i < pathPoints.size()-1; i++) {
            if((pathPoints.get(i).level ==  pathPoints.get(i+1).level) && pathPoints.get(i).level == referencePoint.level) {
                Double[] dxy = pointToSegment(referencePoint, pathPoints.get(i), pathPoints.get(i + 1));

                if (dxy[0] < minD) {
                    minD = dxy[0];
                    closestX = dxy[1];
                    closestY = dxy[2];
                }
            }
        }
        Double frac = reverseInterp(new Position(closestX.floatValue(), closestY.floatValue(), referencePoint.level));
        return new Double[]{minD, frac, closestX, closestY};
    }

    /**
     * Given a point on a path, find the fraction along the path that it is found
     * @param point the point that will be interpolated.
     * @return a number between 0 and 1
     */
    private Double reverseInterp(Position point) {
        for (int i = 0; i < pathPoints.size()-1; i++) {
            if ((point.x>= pathPoints.get(i).x && point.x<= pathPoints.get(i+1).x || point.x>= pathPoints.get(i+1).x && point.x<= pathPoints.get(i).x) &&
                    (point.y>= pathPoints.get(i).y && point.y<= pathPoints.get(i+1).y || point.y>= pathPoints.get(i+1).y && point.y<= pathPoints.get(i).y))
            {
                float dx  = point.x - pathPoints.get(i).x;
                float dy = point.y - pathPoints.get(i).y;
                return pathFractions.get(i) + Math.sqrt(dx*dx + dy*dy)/pathLength;
            }
        }
        throw new IndexOutOfBoundsException("supplied point does not lie on the path");
    }


    /**
     * The distance between a point and a line segment
     * http://stackoverflow.com/a/6853926/5890940
     */
    private Double[] pointToSegment(Position point, Position segmentEnd1, Position segmentEnd2)
    {
        float A = point.x - segmentEnd1.x;
        float B = point.y - segmentEnd1.y;
        float C = segmentEnd2.x - segmentEnd1.x;
        float D = segmentEnd2.y - segmentEnd1.y;

        float dot = A * C + B * D;
        float len_sq = C * C + D * D;
        float param = dot / len_sq;

        float xx, yy;

        if (param < 0) {
            xx = segmentEnd1.x;
            yy = segmentEnd1.y;
        }
        else if (param > 1) {
            xx = segmentEnd2.x;
            yy = segmentEnd2.y;
        } else {
            xx = segmentEnd1.x + param * C;
            yy = segmentEnd1.y + param * D;
        }

        float dx = point.x - xx;
        float dy = point.y - yy;
        return new Double[]{Math.sqrt(dx * dx + dy * dy), (double)xx, (double)yy};
    }


    public Position get(int location)
    {
        return pathPoints.get(location);
    }

    public int size() {
        return pathPoints.size();
    }

    public class Description implements  Comparable<Description>{
        public Shop shop;
        public int entranceNumber;
        public double pathfraction;
        public float pathX;
        public float pathY;
        public double pathDist;


        public Description(Shop shop, int entranceNumber, float pathX, float pathY, double pathDist, double pathfraction) {
            this.shop = shop;
            this.entranceNumber = entranceNumber;
            this.pathX = pathX;
            this.pathY = pathY;
            this.pathDist = pathDist;
            this.pathfraction = pathfraction;
        }

        @Override
        public int compareTo(Description another) {
            if (pathfraction >another.pathfraction) {
                return 1;
            }
            else if (pathfraction <another.pathfraction) {
                return -1;
            }
            else {
                return 0;
            }
        }
    }


}


