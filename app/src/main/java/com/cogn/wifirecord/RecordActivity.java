package com.cogn.wifirecord;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class RecordActivity extends Activity
    implements PopupMenuDialogFragment.OptionSetListener,
        ScrollImageView.RecordMenuMaker,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String RO_RECORD = "Record Wifi at this Point";
    private static final String RO_DELETE = "Delete this point";
    private static final String TAG = "WIFI";
    private static final int REQUEST_CODE_SEARCH_SHOP = 1001;
    private static final int REQUEST_CODE_LOAD_TEST = 1000;
    private static final String PREF_AUTOSCROLL = "com.cogn.wifirecord.PREF_AUTOSCROLL";
    private static final String PREF_SHOWDEBUG = "com.cogn.wifirecord.PREF_SHOWDEBUG";

    public static String sessionStartTime = null;
    private PopupMenuDialogFragment popupMenu;
    private Menu optionsMenu;
    private ScrollImageView floorMapView;
    private SharedPreferences mPrefs;

    private Map<String, FloorPlanImageList> floorplans = new HashMap<>();
    private Map<String, ConnectionPoints> locationConnectionPoints = new HashMap<>();
    private ArrayList<String> recordOptions = new ArrayList<>(Arrays.asList(RO_RECORD, RO_DELETE));
    private String currentPlan;
    private String currentLevel;
    private WifiManager wifiManager;

    private int currentLevelID = -1000;

    // For movement detection
    private SensorManager sensorMan;
    private Sensor accelerometer;
    private long lastLocationClickTime = 0;
    private boolean secondClickTookPlace;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO : Check SmartNavi
        // Data
        floorplans.put("Greenstone", new FloorPlanImageList());
        floorplans.get("Greenstone").add(0, "Lower Level", R.drawable.greenstone_lower);
        floorplans.get("Greenstone").add(1, "Upper Level", R.drawable.greenstone_upper);

        floorplans.put("Home", new FloorPlanImageList());
        floorplans.get("Home").add(0, "Downstairs", R.drawable.house_lower);
        floorplans.get("Home").add(1, "Upstairs", R.drawable.house_upper);

        locationConnectionPoints.put("Greenstone", new ConnectionPoints());
        locationConnectionPoints.get("Greenstone").add(0, 530, 320, 1,570, 660);
        locationConnectionPoints.get("Greenstone").add(0, 1020, 425, 1,1100, 690);
        locationConnectionPoints.put("Home", new ConnectionPoints());
        locationConnectionPoints.get("Home").add(0, 360, 490, 1,252, 54);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Defaults:
        String levelDescriptionToUse;
        if (savedInstanceState!=null) {
            currentPlan = savedInstanceState.getString("currentPlan");
            levelDescriptionToUse = savedInstanceState.getString("currentLevel");
        } else {
            currentPlan = mPrefs.getString("currentPlan", "Home");
            levelDescriptionToUse =  mPrefs.getString("currentLevel", "Downstairs");
        }

        if (sessionStartTime==null) {
            Calendar c = Calendar.getInstance();
            sessionStartTime = DataReadWrite.timeStampFormat.format(c.getTime());
        }

        // WIFI Manager
        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);

        // Sensor manager
        sensorMan = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //Add floormap view
        setContentView(R.layout.activity_record);
        LinearLayout myLayout = (LinearLayout)findViewById(R.id.recordLayout);

        floorMapView = new ScrollImageView(this, this);
        floorMapView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        myLayout.addView(floorMapView);

        FragmentManager fm = getFragmentManager();
        GlobalDataFragment globalData = (GlobalDataFragment) fm.findFragmentByTag("data");
        // create the fragment and data the first time
        if (globalData == null) {
            // add the fragment
            globalData = new GlobalDataFragment();
            fm.beginTransaction().add(globalData, "data").commit();

            GlobalDataFragment.shopDirectory = new ShopDirectory();
            GlobalDataFragment.shopDirectory.loadFromFile(getResources().openRawResource(R.raw.shop_locations));
            GlobalDataFragment.mallGraph = new Graph();
            GlobalDataFragment.mallGraph.loadFromFile(getResources().openRawResource(R.raw.greenstone_graph), 4.2);
            GlobalDataFragment.offlineWifiScanner = null;
            GlobalDataFragment.storedLocationInfo = new StoredLocationInfo(locationConnectionPoints.get(currentPlan), getSummaryInputStream());
        }

        setLevel(levelDescriptionToUse);
        floorMapView.setAutoScroll(mPrefs.getBoolean(PREF_AUTOSCROLL, true));
        floorMapView.setShowDebug(mPrefs.getBoolean(PREF_SHOWDEBUG, true));
        if (savedInstanceState!=null) {
            Bundle floorMapViewState = savedInstanceState.getBundle("floorMapViewState");
            floorMapView.setState(floorMapViewState);
        }
    }


    /**
     * The points at which WIFI fingerprints are known.
     */
    private InputStream getSummaryInputStream() {
        InputStream inputStream;
        if (currentPlan.equalsIgnoreCase("greenstone")) {
            inputStream = getResources().openRawResource(R.raw.greenstone_summary);
        } else if (currentPlan.equalsIgnoreCase("home")) {
            inputStream = getResources().openRawResource(R.raw.home_summary);
        } else {
            throw new IndexOutOfBoundsException("Not a known location");
        }
        return inputStream;
    }


    private InputStream getMacInputStream() {
        InputStream inputStream;
        if (currentPlan.equalsIgnoreCase("greenstone")) {
            inputStream = getResources().openRawResource(R.raw.greenstone_macs);
        } else if (currentPlan.equalsIgnoreCase("home")) {
            inputStream = getResources().openRawResource(R.raw.home_macs);
        } else {
            throw new IndexOutOfBoundsException("Not a known location");
        }
        return inputStream;
    }


    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        if (GlobalDataFragment.locator==null) {
            Log.d("LOC", "no previous locator found, starting a new one");
            GlobalDataFragment.locator = new RecordForLocation(getLocationParameters(currentPlan),
                    this, new WifiScanner(wifiManager, getMacInputStream()));
        } else if (GlobalDataFragment.offlineWifiScanner!=null) {
                Log.d("LOC","offline scanner found, using that");
                GlobalDataFragment.locator.resetReferences(this, GlobalDataFragment.offlineWifiScanner);
        } else {
            Log.d("LOC","No locator started, becasue one is already running");
            GlobalDataFragment.locator.resetReferences(this, new WifiScanner(wifiManager, getMacInputStream()));
        }
        sensorMan.registerListener(GlobalDataFragment.locator, accelerometer, SensorManager.SENSOR_DELAY_UI);
        GlobalDataFragment.locator.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (GlobalDataFragment.locator!=null) {
            sensorMan.unregisterListener(GlobalDataFragment.locator);
            GlobalDataFragment.locator.stop();
            GlobalDataFragment.locator.clearReferences();
        }
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the popupMenu; this adds items to the action bar if it is present.
        optionsMenu = menu;
        getMenuInflater().inflate(R.menu.record, menu);

        optionsMenu.findItem(R.id.menu_auto_scroll).setChecked(mPrefs.getBoolean(PREF_AUTOSCROLL, true));
        optionsMenu.findItem(R.id.menu_show_debug).setChecked(mPrefs.getBoolean(PREF_SHOWDEBUG, true));

        if (GlobalDataFragment.continuousLocate)
            optionsMenu.findItem(R.id.menu_locate).setIcon(R.drawable.ic_menu_mylocation_green);
        else
            optionsMenu.findItem(R.id.menu_locate).setIcon(R.drawable.ic_menu_mylocation);
        return true;
    }

    public void updateRecordProgress(String newText) {
        floorMapView.updateRecordProgress(newText);
    }

    /**
     *
     * @return the id number of the current level
     */
    public int getLevelID(){return currentLevelID;}

    /**
     * For use by the locator to run on the UI thread.
     */
    public void updateLocateProgress(List<String> scores, float currentX, float currentY,
                                     float bestGuessX, float bestGuessY, float bestGuessRadius,
                                     boolean centerViewOnCurrent) {
        floorMapView.updateLocateProgress(scores, currentX, currentY, bestGuessX, bestGuessY, bestGuessRadius, centerViewOnCurrent);
    }

    public void updateMovementStatus(String movementStatus) {
        floorMapView.updateMovementStatus(movementStatus);
    }

    public void setScanFinished() {
        floorMapView.setRecordScanFinished();
    }

    private void updateFloorplan() {
        Bitmap floorMapImage = BitmapFactory.decodeResource(this.getResources(),
                floorplans.get(currentPlan).GetResource(currentLevel));

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density; // Later use this to get the scale image size.  Real pixels * density.
        floorMapView.setImage(floorMapImage, density, currentLevelID);
    }


    /**
     *
     * @param x actual pixel location of recording.  screen x needs to adjusted
     */
    public void makeRecording(final float x, final float y, final int level, final int delay) {
        String nStr = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_number_of_scans), "20");
        final int N = Integer.parseInt(nStr);
        final WifiStrengthRecorder wifiRecorder = new WifiStrengthRecorder(currentPlan, wifiManager, this);
        new Thread(new Runnable() {
            public void run() {
                wifiRecorder.MakeRecording(x, y, level, N, delay);
            }
        }).start();
    }

    private String getPref(int id){
        return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(id), null);
    }

    public RecordForLocation.Parameters getLocationParameters(String location)
    {
        switch (location) {
            case "Home":
            {
                float pxPerM = 38.0f;
                float walkingPace =  2.0f; // m/s FAST: 7.6km/h;
                float errorAccommodationM = 0.0f; // Distance that is allowed to move in zero time
                int lengthMovingObs = 3;
                int minLengthStationaryObs = 5;
                int maxLengthStationaryObs = 20;
                boolean updateForSamePos = true;
                float stickyMinImprovement = 5.0f; // The amount by which the new score must be better than the last during the sticky period
                int stickyMaxTime = 3000;
                return new RecordForLocation().new Parameters(pxPerM,walkingPace,errorAccommodationM,lengthMovingObs,
                        minLengthStationaryObs,maxLengthStationaryObs,updateForSamePos,stickyMinImprovement,stickyMaxTime);
            }
            case "Greenstone":
            {
                float pxPerM = 4.2f;
                float walkingPace =  Float.parseFloat(getPref(R.string.key_location_walking_pace));
                float errorAccommodationM = Float.parseFloat(getPref(R.string.key_location_error_accommodation));
                int lengthMovingObs = Integer.parseInt(getPref(R.string.key_location_length_moving_obs));
                int minLengthStationaryObs = Integer.parseInt(getPref(R.string.key_location_min_length_stationary_obs));
                int maxLengthStationaryObs = Integer.parseInt(getPref(R.string.key_location_max_length_stationary_obs));
                boolean updateForSamePos = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_location_update_same_place), true);
                float stickyMinImprovement = Float.parseFloat(getPref(R.string.key_location_sticky_min_improvement));
                int stickyMaxTime = Integer.parseInt(getPref(R.string.key_location_sticky_max_time));
                return new RecordForLocation().new Parameters(pxPerM,walkingPace,errorAccommodationM,lengthMovingObs,
                        minLengthStationaryObs,maxLengthStationaryObs,updateForSamePos,stickyMinImprovement,stickyMaxTime);
           }
            default: {
                return null;
            }
        }

    }

    /**
     * Implemented outside where the button is pressed so that tests can also access it.
     */
    public void startLocating(ProvidesWifiScan wifiScanner){
        MenuItem menuItem = optionsMenu.findItem(R.id.menu_auto_scroll);
        menuItem.setEnabled(true);
        boolean autoScroll = menuItem.isChecked();
        floorMapView.setAutoScroll(autoScroll);
        if (GlobalDataFragment.locator!=null) {
            sensorMan.unregisterListener(GlobalDataFragment.locator);
            GlobalDataFragment.locator.clearReferences();
            GlobalDataFragment.locator.stop();
        }
        GlobalDataFragment.locator = new RecordForLocation(getLocationParameters(currentPlan), this, wifiScanner);
        sensorMan.registerListener(GlobalDataFragment.locator, accelerometer, SensorManager.SENSOR_DELAY_UI);
        GlobalDataFragment.locator.start();
    }


    /**
     * Item selected in main action bar popupMenu
     *
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.menu_locate: {
                long clickDelay = (Calendar.getInstance().getTimeInMillis() - lastLocationClickTime);
                Log.d(TAG,""+clickDelay);
                if (clickDelay<600){
                    secondClickTookPlace = true;
                    GlobalDataFragment.continuousLocate = !GlobalDataFragment.continuousLocate;
                    invalidateOptionsMenu();
                } else {  // Single click.  use that to reset the location under continuous mode or show location otherwise
                    secondClickTookPlace = false;
                    if (!GlobalDataFragment.continuousLocate) {
                        GlobalDataFragment.locator.sendLocation();
                    }
                    Timer timer = new Timer();
                    TimerTask resetLocationIfNoSecondClick = new TimerTask() {
                        @Override
                        public void run() {
                            if (secondClickTookPlace) {
                                Log.d(TAG, "600ms later.  locate reset cancelled");
                            } else {
                                Log.d(TAG, "600ms later.  Locate reset");
                                GlobalDataFragment.locator.reset();
                            }
                            
                        }
                    };
                    timer.schedule(resetLocationIfNoSecondClick, 600);
                }
                lastLocationClickTime = Calendar.getInstance().getTimeInMillis();

                // Start the locating thread
                //startLocating(new WifiScanner(wifiManager, getMacInputStream()));
                return true;
            }
            case R.id.menu_search: {
                Intent intent = new Intent(this, SearchShopActivity.class);
                intent.putExtra("location", currentPlan);
                startActivityForResult(intent, REQUEST_CODE_SEARCH_SHOP);
                return true;
            }
            case R.id.menu_show_debug: {
                // Start the locating thread
                MenuItem menuItem = optionsMenu.findItem(R.id.menu_show_debug);
                boolean showDebug = !menuItem.isChecked();
                menuItem.setChecked(showDebug);
                floorMapView.setShowDebug(showDebug);
                SharedPreferences.Editor ed = mPrefs.edit();
                ed.putBoolean(PREF_SHOWDEBUG, showDebug);
                ed.commit();
                return true;
            }
            case R.id.menu_auto_scroll: {
                // Start the locating thread
                MenuItem menuItem = optionsMenu.findItem(R.id.menu_auto_scroll);
                boolean autoScroll = !menuItem.isChecked();
                menuItem.setChecked(autoScroll);
                floorMapView.setAutoScroll(autoScroll);
                SharedPreferences.Editor ed = mPrefs.edit();
                ed.putBoolean(PREF_AUTOSCROLL, autoScroll);
                ed.commit();
                return true;
            }
            case R.id.menu_clear_route: {
                GlobalDataFragment.latestRoute = null;
                floorMapView.invalidate();
                return true;
            }
            case R.id.menu_continuous_record: {
                Intent intent = new Intent(this, ContinuousRecordActivity.class);
                intent.putExtra("location", currentPlan);
                startActivity(intent);
                return true;
            }
            case R.id.action_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.menu_select_location: {
                // TODO: Mustn't change location while locator is running.
                popupMenu = new PopupMenuDialogFragment();
                Bundle options = new Bundle();
                options.putStringArrayList("options", new ArrayList<>(floorplans.keySet()));
                options.putString("type", "location");
                popupMenu.setArguments(options);
                popupMenu.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
                popupMenu.show(getFragmentManager(), "menu");
                return true;
            }
            case R.id.menu_manual_record: {
                Intent intent = new Intent(this, ManualRecordActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.menu_select_level: {
                popupMenu = new PopupMenuDialogFragment();
                Bundle options = new Bundle();
                options.putStringArrayList("options", floorplans.get(currentPlan).descriptions);
                options.putString("type", "level");
                popupMenu.setArguments(options);
                popupMenu.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
                popupMenu.show(getFragmentManager(), "menu");
                return true;
            }
            case R.id.menu_test: {
                Intent intent = new Intent(this, LoadTestActivity.class);
                intent.putExtra("location", currentPlan);
                startActivityForResult(intent, REQUEST_CODE_LOAD_TEST);
                //runSimulatedPath();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LOAD_TEST && resultCode == RESULT_OK && data != null) {
            String filename = data.getStringExtra(LoadTestActivity.EXTRA_FILENAME);
            runSimulatedPath(filename);
            return;
        }
        if (requestCode == REQUEST_CODE_SEARCH_SHOP && resultCode == RESULT_OK && data != null) {
            boolean directions = data.getBooleanExtra(SearchShopActivity.INTENT_EXTRA_DIRECTIONS, false);
            String category = data.getStringExtra(SearchShopActivity.INTENT_EXTRA_CATEGORY);
            String shopName = data.getStringExtra(SearchShopActivity.INTENT_EXTRA_SHOP);
            Log.d(TAG, "" + directions);
            Log.d(TAG, category);
            Log.d(TAG, shopName);
            Shop shop = GlobalDataFragment.shopDirectory.getShop(category, shopName);
            for (int i = 0; i < shop.getEntranceLocations().size(); i++) {
                floorMapView.addShop(shopName,
                        shop.getEntranceLocations().get(i).x,
                        shop.getEntranceLocations().get(i).y,
                        shop.getEntranceLocations().get(i).level);
            }
            Position currentPosition = floorMapView.getCurrentPosition();
            if (currentPosition==null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setPositiveButton("OK", null);
                builder.setTitle("Error");
                builder.setMessage("No current position has been set");
                builder.show();
                return;
            }
                else {

                Route route = GlobalDataFragment.mallGraph.getRoute(floorMapView.getCurrentPosition(), shop);
                GlobalDataFragment.latestRoute = route;
                floorMapView.invalidate();
            }
        }

    }

    /**
     * Replaces the wifi readings with readings from a file and starts locating
     */
    private void runSimulatedPath(String pathFilename) {
        Log.d(TAG,"Getting summary macs");
        String macFilename = pathFilename.replace("path", "macs");
        floorMapView.updateMovementStatus("Getting summary macs");
        MacLookup summaryMacs = new MacLookup(getMacInputStream());
        Log.d(TAG,"Getting path macs");
        floorMapView.updateMovementStatus("Getting path macs");
        MacLookup pathMacs = new MacLookup("Greenstone", macFilename);

        Log.d(TAG,"Processing path");
        floorMapView.updateMovementStatus("Processing path");
        GlobalDataFragment.offlineWifiScanner = new OfflineWifiScanner(pathFilename, "Greenstone" , summaryMacs, pathMacs,
                Calendar.getInstance().getTimeInMillis());

        Log.d(TAG,"Starting simulation");
        floorMapView.updateMovementStatus("Starting simulation");
        setLocation("Greenstone");
        startLocating(GlobalDataFragment.offlineWifiScanner);
    }


    /**
     * Sets the image and dots etc.
     * @param newLevelID
     */
    public void setLevel(int newLevelID)
    {
        if (newLevelID!=currentLevelID) {
            currentLevelID = newLevelID;
            currentLevel = floorplans.get(currentPlan).DescriptionFromID(newLevelID);
            updateFloorplan();

            floorMapView.setPreviousPoints(GlobalDataFragment.storedLocationInfo.getXList(currentLevelID),
                    GlobalDataFragment.storedLocationInfo.getYList(currentLevelID));
            floorMapView.invalidate();

            SharedPreferences.Editor ed = mPrefs.edit();
            ed.putString("currentLevel", currentLevel);
            ed.commit();
        }
    }

    /**
     * Gets the id and calls  {@link #setLevel(int) setLevel}
     * @param newLevelDescription
     */
    public void setLevel(String newLevelDescription)
    {
        setLevel(floorplans.get(currentPlan).IDFromDescription(newLevelDescription));
    }

    public void setLocation(String value) {
        currentPlan = value;

        // read the files that store previous points
        // TODO: If this ends up being slow do it on another thread.
        GlobalDataFragment.storedLocationInfo = new StoredLocationInfo(locationConnectionPoints.get(currentPlan), getSummaryInputStream());
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putString("currentPlan", currentPlan);
        ed.commit();
        setLevel(floorplans.get(currentPlan).GetDefaultLevel());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentPlan", currentPlan);
        outState.putString("currentLevel", currentLevel);
        outState.putBundle("floorMapViewState", floorMapView.getState());
    }

    /**
     * This handles the button clicking on the submenus that pop up for various reasons.
     * @param results
     */
    @Override
    public void onOptionSet(Bundle results) {
        String type = results.getString("type");
        switch (type) {
            case "location":
                if (!currentPlan.equals(results.getString("value"))) {
                    setLocation(results.getString("value"));
                }
                popupMenu.dismiss();
                return;
            case "level":
                if (!currentLevel.equals(results.getString("value"))) {
                    setLevel(results.getString("value"));
                }
                popupMenu.dismiss();
                return;
            case "record":
                String value = results.getString("value");
                switch (value) {
                    case RO_RECORD:
                        makeRecording(results.getFloat("x"), results.getFloat("y"), currentLevelID, 500);
                        break;
                    case RO_DELETE:
                        floorMapView.deletePoint();
                        break;
                    default:
                        throw new IllegalArgumentException("Not a known menu option: " + value);
                }
                popupMenu.dismiss();
        }
    }

    /**
     * Make a popupMenu at the location of a second click on the floormap view, asking the user if they
     * want to make a wifi recfording there or remove the point.
     *
     * @param x actual pixel location of click event.  Not device independent version.
     * @param y actual pixel location of click event.  Not device independent version.
     */
    @Override
    public void makeRecordMenu(float x, float y) {
        popupMenu = new PopupMenuDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putStringArrayList("options", recordOptions);
        bundle.putString("type", "record");
        bundle.putFloat("x", x);
        bundle.putFloat("y", y);
        popupMenu.setArguments(bundle);
        popupMenu.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        popupMenu.show(getFragmentManager(), "menu");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("TAG",key);
        if (key.equals(getString(R.string.key_number_of_scans))) {
            String nStr = sharedPreferences.getString(getString(R.string.key_number_of_scans), "5");
            Log.d("TAG",nStr);
        }

    }

    /**
     * Keeps the floormaps indexed by the location name and then a number and description for the levels.
     */
    private class FloorPlanImageList{
        private List<Integer> ids = new ArrayList<>();
        public ArrayList<String> descriptions = new ArrayList<String>();
        private List<Integer> resourceIDs = new ArrayList<>();
        public FloorPlanImageList(){}
        public void add(int id, String description, int resourceID)
        {
            this.ids.add(id);
            this.descriptions.add(description);
            this.resourceIDs.add(resourceID);
        }
        public int IDFromDescription(String description){
            Integer pos = descriptions.indexOf(description);
            if (pos>=0) return ids.get(pos);
            else throw new ArrayIndexOutOfBoundsException("provided description does not exist in the list");
        }
        public String DescriptionFromID(Integer id){
            Integer pos = ids.indexOf(id);
            if (pos>=0) return descriptions.get(pos);
            else throw new ArrayIndexOutOfBoundsException("provided description does not exist in the list");
        }
        public Integer GetResource(Integer id)
        {
            Integer pos = ids.indexOf(id);
            if (pos>=0) return resourceIDs.get(pos);
            else throw new ArrayIndexOutOfBoundsException("provided id does not exist in the list");
        }
        public Integer GetResource(String description)
        {
            Integer pos = descriptions.indexOf(description);
            if (pos>=0) return resourceIDs.get(pos);
            else throw new ArrayIndexOutOfBoundsException("provided description does not exist in the list");
        }
        public String GetDefaultLevel()
        {
            return descriptions.get(0);
        }
    }
}
