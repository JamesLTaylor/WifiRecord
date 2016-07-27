package com.cogn.wifirecord;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;


/**
 * Eventually this will store all the data outside the apk to allow shops to be added without updating
 */
public class ShoppingCenter {
    //private SharedPreferences appPreferences;
    //private Resources appResources;

    private String path;
    private String name;
    private double pxPerM;
    private Map<Integer, String> levelMapping;
    private Map<Integer, String> levelImageFilenames;
    private ConnectionPoints connectionPoints;
    private ShopDirectory shopDirectory;
    private Graph mallGraph;

    private String wifiFingerPrintFilename;
    private String wifiMacsFilename;
    private String mallGraphFilename;
    private String shopDirectoryFilename;
    private String pathDescriptionsFilename;

    public static void populateGlobalCenterList(){
        //TODO: Get from folder
        GlobalDataFragment.centerNamesAndFolders = new HashMap<>();
        GlobalDataFragment.centerNamesAndFolders.put("Greenstone", "greenstone");
        //GlobalDataFragment.centerNamesAndFolders.put("Home", "home");
    }

    public ShoppingCenter(Resources appResources, String name){
        this.name = name;
        path = GlobalDataFragment.centerNamesAndFolders.get(name);

        levelMapping = new HashMap<>();
        levelImageFilenames = new HashMap<>();
        updateFromXML(appResources, path);

        connectionPoints = new ConnectionPoints();
        connectionPoints.add(0, 530, 320, 1,570, 660);
        connectionPoints.add(0, 1020, 425, 1,1100, 690);

        shopDirectory = new ShopDirectory();
        shopDirectory.loadFromFile(getStreamFromFilename(shopDirectoryFilename, appResources));

        mallGraph = new Graph();
        mallGraph.loadFromFile(getStreamFromFilename(mallGraphFilename, appResources), pxPerM);

    }

    private InputStream getStreamFromFilename(String filename, Resources appResources) {
        InputStream istr = null;
        try {
            istr = appResources.getAssets().open(path + "/" + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return istr;
    }


    public static String getDefaultCenter() {
        return "Greenstone";
    }

    private void updateFromXML(Resources appResources, String folderName){
        InputStream xmlStream;
        try {
            xmlStream = appResources.getAssets().open(folderName + "/details.xml");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        XmlPullParser xmlParser = Xml.newPullParser();
        Reader reader = new InputStreamReader(xmlStream);
        try {
            xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            xmlParser.setInput(reader);
            int event = xmlParser.getEventType();
            String text = null;
            String name;
            while (event != XmlPullParser.END_DOCUMENT) {
                name = xmlParser.getName();
                switch (event) {
                    case XmlPullParser.START_TAG:
                    {
                        if (name.equals("item")) {
                            String levelName = xmlParser.getAttributeValue(null, "name");
                            String levelImageResource = xmlParser.getAttributeValue(null, "resource");
                            int id = Integer.parseInt(xmlParser.getAttributeValue(null, "id"));
                            levelMapping.put(id, levelName);
                            levelImageFilenames.put(id, levelImageResource);
                        }
                        break;
                    }
                    case XmlPullParser.TEXT: {
                        text = xmlParser.getText();
                        break;
                    }
                    case XmlPullParser.END_TAG:
                        if (name.equals("name")) {
                            name = text;
                        }
                        if (name.equals("pxPerM")) {
                            pxPerM = Double.parseDouble(text);
                        }
                        if (name.equals("wifiFingerPrint")) {
                            wifiFingerPrintFilename = text;
                        }
                        if (name.equals("wifiMacs")) {
                            wifiMacsFilename = text;
                        }
                        if (name.equals("mallGraph")) {
                            mallGraphFilename = text;
                        }
                        if (name.equals("shopDirectory")) {
                            shopDirectoryFilename = text;
                        }
                        if (name.equals("pathDescriptions")) {
                            pathDescriptionsFilename = text;
                        }
                        break;
                }
                event = xmlParser.next();
            }
        }
        catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String[] getListOfCenterNames(){
        int nCenters = GlobalDataFragment.centerNamesAndFolders.size();
        String[] result = new String[nCenters];
        result = GlobalDataFragment.centerNamesAndFolders.keySet().toArray(result);
        return result;
    }

    public int getLevel(String levelString) {
        for (Map.Entry<Integer, String> entry : levelMapping.entrySet()) {
            if (levelString.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException(levelString + " does not exist in " + name);
    }

    public String[] getLevels(){
        String[] levels = new String[levelMapping.values().size()];
        levelMapping.values().toArray(levels);
        return levels;
    }

    public Bitmap getImage(int level, Resources appResources)
    {
        AssetManager assetManager = appResources.getAssets();
        InputStream istr = null;
        try {
            istr = assetManager.open(path+"/"+levelImageFilenames.get(level));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return BitmapFactory.decodeStream(istr);

    }

    public ShopDirectory getShopDirectory(){
        return shopDirectory;
    }

    public Graph getMallGraph(){
        return mallGraph;
    }

    public ConnectionPoints getConnectionPoints(){
        return connectionPoints;
    }

    /**
     * The points at which WIFI fingerprints are known.
     */
    public InputStream getWifiFingerPrints(Resources appResources) {
        return getStreamFromFilename(wifiFingerPrintFilename, appResources);
    }

    public InputStream getMacInputStream(Resources appResources) {
        return getStreamFromFilename(wifiMacsFilename, appResources);
    }

    public InputStream getPathDescriptions(Resources appResources) {
        return getStreamFromFilename(pathDescriptionsFilename, appResources);
    }


    public RecordForLocation.Parameters getLocationParameters(SharedPreferences appPreferences, Resources appResources) {
        float walkingPace =  Float.parseFloat(appPreferences.getString(appResources.getString(R.string.key_location_walking_pace), "2.0"));
        float errorAccommodationM = Float.parseFloat(appPreferences.getString(appResources.getString(R.string.key_location_error_accommodation), "0.0"));
        int lengthMovingObs = Integer.parseInt(appPreferences.getString(appResources.getString(R.string.key_location_length_moving_obs), "3"));
        int minLengthStationaryObs = Integer.parseInt(appPreferences.getString(appResources.getString(R.string.key_location_min_length_stationary_obs), "5"));
        int maxLengthStationaryObs = Integer.parseInt(appPreferences.getString(appResources.getString(R.string.key_location_max_length_stationary_obs), "20"));
        boolean updateForSamePos = appPreferences.getBoolean(appResources.getString(R.string.key_location_update_same_place), true);
        float stickyMinImprovement = Float.parseFloat(appPreferences.getString(appResources.getString(R.string.key_location_sticky_min_improvement), "5.0"));
        int stickyMaxTime = Integer.parseInt(appPreferences.getString(appResources.getString(R.string.key_location_sticky_max_time), "3000"));

        return new RecordForLocation().new Parameters((float)pxPerM, walkingPace, errorAccommodationM, lengthMovingObs,
                minLengthStationaryObs, maxLengthStationaryObs, updateForSamePos, stickyMinImprovement, stickyMaxTime);

    }

    public int getDefaultLevel() {
        return 0;
    }

    public String getPathName() {
        return path;
    }

    public String getName() {
        return name;
    }
}


