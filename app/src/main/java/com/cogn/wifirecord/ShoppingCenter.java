package com.cogn.wifirecord;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingCenter {
    private SharedPreferences appPreferences;
    private Resources appResources;
    private Map<Integer, String> levelMapping;
    private List<Integer> drawableIDs;
    private ConnectionPoints connectionPoints;
    private int wifiFingerPrintFileID;
    private int wifiMacsFileID;
    private String descriptionShort;

    public static String[] getListOfCenterNames(){
        return new String[]{"Home", "Greenstone"};
    }

    public static ShoppingCenter makeFor(Resources appResources, SharedPreferences appPreferences, String name){
        ShoppingCenter center = new ShoppingCenter();
        center.appPreferences = appPreferences;
        center.appResources = appResources;
        center.levelMapping = new HashMap<>();
        center.drawableIDs = new ArrayList<>();

        if (name.equals("Greenstone")){
            center.descriptionShort = "Greenstone";
            center.levelMapping.put(0, "Lower Level");
            center.drawableIDs.add(R.drawable.greenstone_lower);
            center.levelMapping.put(1, "Upper Level");
            center.drawableIDs.add(R.drawable.greenstone_upper);
            center.connectionPoints = new ConnectionPoints();
            center.connectionPoints.add(0, 530, 320, 1,570, 660);
            center.connectionPoints.add(0, 1020, 425, 1,1100, 690);
            center.wifiFingerPrintFileID = R.raw.greenstone_summary;
            center.wifiMacsFileID = R.raw.greenstone_macs;


        } else if (name.equals("Home")) {
            //floorplans.put("Home", new FloorPlanImageList());
            //floorplans.get("Home").add(0, "Downstairs", R.drawable.house_lower);
            //floorplans.get("Home").add(1, "Upstairs", R.drawable.house_upper);
            //locationConnectionPoints.put("Home", new ConnectionPoints());
            //locationConnectionPoints.get("Home").add(0, 360, 490, 1,252, 54);

        } else {
            throw new IllegalArgumentException("Unknown shopping center: " + name);
        }
        return center;
    }

    public int getLevel(String levelString) {
        for (Map.Entry<Integer, String> entry : levelMapping.entrySet()) {
            if (levelString.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException(levelString + " does not exist in " + descriptionShort);
    }

    public String getLevelString(int level){
        return levelMapping.get(level);
    }

    public String[] getLevels(){
        String[] levels = new String[levelMapping.values().size()];
        levelMapping.values().toArray(levels);
        return levels;
    }

    public Bitmap getImage(int level)
    {
        return BitmapFactory.decodeResource(appResources, drawableIDs.get(level));
    }

    public String[] getPaths()
    {
        return null;
    }

    public ShopDirectory getShopDirectory(){
        return null;
    }

    public static Graph getMallGraph(){
        return null;
    }

    public ConnectionPoints getConnectionPoints(){
        return connectionPoints;
    }

    /**
     * The points at which WIFI fingerprints are known.
     */
    public InputStream getSummaryInputStream() {
        return appResources.openRawResource(wifiFingerPrintFileID);
    }


    public InputStream getMacInputStream() {
        return appResources.openRawResource(wifiMacsFileID);
    }


    public RecordForLocation.Parameters getLocationParameters() {
        switch (descriptionShort) {
            case "Home": {
                float pxPerM = 38.0f;
                float walkingPace = 2.0f; // m/s FAST: 7.6km/h;
                float errorAccommodationM = 0.0f; // Distance that is allowed to move in zero time
                int lengthMovingObs = 3;
                int minLengthStationaryObs = 5;
                int maxLengthStationaryObs = 20;
                boolean updateForSamePos = true;
                float stickyMinImprovement = 5.0f; // The amount by which the new score must be better than the last during the sticky period
                int stickyMaxTime = 3000;
                return new RecordForLocation().new Parameters(pxPerM, walkingPace, errorAccommodationM, lengthMovingObs,
                        minLengthStationaryObs, maxLengthStationaryObs, updateForSamePos, stickyMinImprovement, stickyMaxTime);
            }
            case "Greenstone": {
                float pxPerM = 4.2f;

                /*float walkingPace = 2.0f; // m/s FAST: 7.6km/h;
                float errorAccommodationM = 0.0f; // Distance that is allowed to move in zero time
                int lengthMovingObs = 3;
                int minLengthStationaryObs = 5;
                int maxLengthStationaryObs = 20;
                boolean updateForSamePos = true;
                float stickyMinImprovement = 5.0f; // The amount by which the new score must be better than the last during the sticky period
                int stickyMaxTime = 3000;
                */

                float walkingPace =  Float.parseFloat(appPreferences.getString(appResources.getString(R.string.key_location_walking_pace), "2.0"));
                float errorAccommodationM = Float.parseFloat(appPreferences.getString(appResources.getString(R.string.key_location_error_accommodation), "0.0"));
                int lengthMovingObs = Integer.parseInt(appPreferences.getString(appResources.getString(R.string.key_location_length_moving_obs), "3"));
                int minLengthStationaryObs = Integer.parseInt(appPreferences.getString(appResources.getString(R.string.key_location_min_length_stationary_obs), "5"));
                int maxLengthStationaryObs = Integer.parseInt(appPreferences.getString(appResources.getString(R.string.key_location_max_length_stationary_obs), "20"));
                boolean updateForSamePos = appPreferences.getBoolean(appResources.getString(R.string.key_location_update_same_place), true);
                float stickyMinImprovement = Float.parseFloat(appPreferences.getString(appResources.getString(R.string.key_location_sticky_min_improvement), "5.0"));
                int stickyMaxTime = Integer.parseInt(appPreferences.getString(appResources.getString(R.string.key_location_sticky_max_time), "3000"));

                return new RecordForLocation().new Parameters(pxPerM, walkingPace, errorAccommodationM, lengthMovingObs,
                        minLengthStationaryObs, maxLengthStationaryObs, updateForSamePos, stickyMinImprovement, stickyMaxTime);
            }
            default: {
                return null;
            }
        }
    }

    public String getDescriptionShort() {
        return descriptionShort;
    }

    public int getDefaultLevel() {
        return 0;
    }
}


