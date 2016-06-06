package com.cogn.wifirecord;

import android.app.Activity;
import android.app.DialogFragment;
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
import android.widget.LinearLayout;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RecordActivity extends Activity
    implements PopupMenuDialogFragment.OptionSetListener,
        ScrollImageView.RecordMenuMaker,
        SharedPreferences.OnSharedPreferenceChangeListener
        {

    static final String RO_RECORD = "Record Wifi at this Point";
    static final String RO_DELETE = "Delete this point";
    private static final String TAG = "WIFI";

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
    private PreviousRecordings levelsAndPoints;
    private StoredLocationInfo storedLocationInfo;
    private RecordForLocation locator = null;
    private int currentLevelID;
    private ScrollImageView.ViewMode viewMode;

    // For movement detection
    private SensorManager sensorMan;
    private Sensor accelerometer;
    private ScrollImageView.ViewMode savedViewModeToUse;
    private int REQUEST_CODE_LOAD_TEST = 1000;

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
            savedViewModeToUse = (ScrollImageView.ViewMode)savedInstanceState.getSerializable("viewMode");
        } else {
            currentPlan = mPrefs.getString("currentPlan", "Home");
            levelDescriptionToUse =  mPrefs.getString("currentLevel", "Downstairs");
            savedViewModeToUse = ScrollImageView.ViewMode.RECORD;
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
        if (savedInstanceState!=null) {
            Bundle floorMapViewState = savedInstanceState.getBundle("floorMapViewState");
            floorMapView.setState(floorMapViewState);
        }
        levelsAndPoints = new PreviousRecordings(currentPlan);
        storedLocationInfo = new StoredLocationInfo(locationConnectionPoints.get(currentPlan), getSummaryInputStream());
        setLevel(levelDescriptionToUse);
        setViewMode(savedViewModeToUse);
    }

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
        if (viewMode==ScrollImageView.ViewMode.LOCATE)
        {
            // TODO: use the old locator with history.
            if (locator==null) {
                locator = new RecordForLocation(getLocationParameters(currentPlan),
                        storedLocationInfo, this, new WifiScanner(wifiManager, getMacInputStream()));
                sensorMan.registerListener(locator, accelerometer, SensorManager.SENSOR_DELAY_UI);
                locator.start();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (viewMode==ScrollImageView.ViewMode.LOCATE)
        {
            sensorMan.unregisterListener(locator);
            locator.Stop();
        }
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the popupMenu; this adds items to the action bar if it is present.
        optionsMenu = menu;
        getMenuInflater().inflate(R.menu.record, menu);
        if (viewMode == ScrollImageView.ViewMode.LOCATE) {
            optionsMenu.findItem(R.id.menu_locate).setEnabled(false);
            optionsMenu.findItem(R.id.menu_record).setEnabled(true);
        } else {
            optionsMenu.findItem(R.id.menu_locate).setEnabled(true);
            optionsMenu.findItem(R.id.menu_record).setEnabled(false);
        }
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
                                     float bestGuessX, float bestGuessY, float bestGuessRadius) {
        floorMapView.updateLocateProgress(scores, currentX, currentY, bestGuessX, bestGuessY, bestGuessRadius);
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
        floorMapView.setImage(floorMapImage, density);
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
        optionsMenu.findItem(R.id.menu_reset_location).setEnabled(true);
        optionsMenu.findItem(R.id.menu_reset_location).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        setViewMode(ScrollImageView.ViewMode.LOCATE);
        locator = new RecordForLocation(getLocationParameters(currentPlan),
                storedLocationInfo, this, wifiScanner);
        sensorMan.registerListener(locator, accelerometer, SensorManager.SENSOR_DELAY_UI);
        locator.start();
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
                // Start the locating thread
                startLocating(new WifiScanner(wifiManager, getMacInputStream()));
                return true;
            }
            case R.id.menu_record: {
                // Start the locating thread
                sensorMan.unregisterListener(locator);
                locator.Stop();
                setViewMode(ScrollImageView.ViewMode.RECORD);
                optionsMenu.findItem(R.id.menu_auto_scroll).setEnabled(false);
                optionsMenu.findItem(R.id.menu_reset_location).setEnabled(false);
                optionsMenu.findItem(R.id.menu_reset_location).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                floorMapView.invalidate();
                return true;
            }
            case R.id.menu_auto_scroll: {
                // Start the locating thread
                MenuItem menuItem = optionsMenu.findItem(R.id.menu_auto_scroll);
                boolean autoScroll = !menuItem.isChecked();
                menuItem.setChecked(autoScroll);
                floorMapView.setAutoScroll(autoScroll);
                return true;
            }
            case R.id.menu_reset_location: {
                locator.Stop();
                floorMapView.setViewMode(ScrollImageView.ViewMode.RECORD);
                floorMapView.invalidate();
                startLocating(new WifiScanner(wifiManager, getMacInputStream()));
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
        }
    }

    /**
     * Replaces the wifi readings with readings from a file and starts locating
     */
    private void runSimulatedPath(String pathFilename) {
        setViewMode(ScrollImageView.ViewMode.LOCATE);
        Log.d(TAG,"Getting summary macs");
        String macFilename = pathFilename.replace("path", "macs");
        floorMapView.updateMovementStatus("Getting summary macs");
        MacLookup summaryMacs = new MacLookup(getMacInputStream());
        Log.d(TAG,"Getting path macs");
        floorMapView.updateMovementStatus("Getting path macs");
        MacLookup pathMacs = new MacLookup("Greenstone", macFilename);

        Log.d(TAG,"Processing path");
        floorMapView.updateMovementStatus("Processing path");
        final OfflineWifiScanner offlineWifiProvider = new OfflineWifiScanner(pathFilename, "Greenstone" , summaryMacs, pathMacs);
        //InputStream inputStream = this.getResources().openRawResource(R.raw.greenstone_continuous_20160511_130140);
        //final OfflineWifiScanner offlineWifiProvider = new OfflineWifiScanner(inputStream);

        Log.d(TAG,"Starting simulation");
        floorMapView.updateMovementStatus("Starting simulation");
        setLocation("Greenstone");
        startLocating(offlineWifiProvider);
    }

    public void setViewMode(ScrollImageView.ViewMode newMode){
        //Only update the option menu if it has already been created.  Seems to happen after onResume which calls this method.
        if (optionsMenu!=null) {
            if (newMode == ScrollImageView.ViewMode.LOCATE) {
                optionsMenu.findItem(R.id.menu_locate).setEnabled(false);
                optionsMenu.findItem(R.id.menu_record).setEnabled(true);
            } else {
                optionsMenu.findItem(R.id.menu_locate).setEnabled(true);
                optionsMenu.findItem(R.id.menu_record).setEnabled(false);
            }
        }
        viewMode = newMode;
        floorMapView.setViewMode(viewMode);
    }


    /**
     * Sets the image and dots etc.
     * @param newLevelID
     */
    public void setLevel(int newLevelID)
    {
        currentLevelID = newLevelID;
        currentLevel = floorplans.get(currentPlan).DescriptionFromID(newLevelID);
        updateFloorplan();

        floorMapView.setPreviousPoints(
                levelsAndPoints.GetXList(currentLevelID), levelsAndPoints.GetYList(currentLevelID),
                storedLocationInfo.getXList(currentLevelID), storedLocationInfo.getYList(currentLevelID));
        floorMapView.invalidate();

        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putString("currentLevel", currentLevel);
        ed.commit();

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
        levelsAndPoints = new PreviousRecordings(currentPlan);
        storedLocationInfo = new StoredLocationInfo(locationConnectionPoints.get(currentPlan), getSummaryInputStream());
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
        outState.putSerializable("viewMode", viewMode);
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
     * @param y
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
