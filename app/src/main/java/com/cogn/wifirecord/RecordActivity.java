package com.cogn.wifirecord;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class RecordActivity extends Activity
    implements PopupMenuDialogFragment.OptionSetListener,
        ScrollImageView.RecordMenuMaker,
        SharedPreferences.OnSharedPreferenceChangeListener, AdapterView.OnItemSelectedListener {

    private static final String RO_RECORD = "Record Wifi at this Point";
    private static final String RO_DELETE = "Delete this point";
    private static final String TAG = "WIFI";

    private static final int REQUEST_CODE_LOAD_TEST = 1000;
    private static final int REQUEST_CODE_SEARCH_SHOP = 1001;
    private static final int REQUEST_CODE_SELECT_CENTER = 1002;

    private static final String PREF_AUTOSCROLL = "com.cogn.wifirecord.PREF_AUTOSCROLL";
    private static final String PREF_SHOWDEBUG = "com.cogn.wifirecord.PREF_SHOWDEBUG";
    private static final String SAVED_SHOPPING_CENTER_NAME = "com.cogn.wifirecord.SAVED_SHOPPING_CENTER_NAME";
    private static final String SAVED_SHOPPING_CENTER_LEVEL = "com.cogn.wifirecord.SAVED_SHOPPING_CENTER_LEVEL";
    private static final String SAVED_FLOORMAP_STATE = "com.cogn.wifirecord.SAVED_FLOORMAP_STATE";

    private PopupMenuDialogFragment popupMenu;
    private Menu optionsMenu;
    private ScrollImageView floorMapView;
    private SharedPreferences mPrefs;

    private ArrayList<String> recordOptions = new ArrayList<>(Arrays.asList(RO_RECORD, RO_DELETE));
    private WifiManager wifiManager;

    private int currentLevelID = -1000;

    // For movement detection
    private SensorManager sensorMan;
    private Sensor accelerometer;
    private long lastLocationClickTime = 0;
    private boolean secondClickTookPlace;
    private Spinner spinnerCurrentLevel;
    public String sessionStartTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO : Check SmartNavi
        // Data

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (sessionStartTime==null) {
            Calendar c = Calendar.getInstance();
            sessionStartTime = DataReadWrite.timeStampFormat.format(c.getTime());
        }

        // WIFI Manager
        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);

        // Sensor manager
        sensorMan = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        String centerName;
        int level;
        if (savedInstanceState!=null) {
            centerName = savedInstanceState.getString(SAVED_SHOPPING_CENTER_NAME);
            level = savedInstanceState.getInt(SAVED_SHOPPING_CENTER_LEVEL);
        } else {
            centerName = mPrefs.getString(SAVED_SHOPPING_CENTER_NAME, ShoppingCenter.getDefaultCenter());
            level = mPrefs.getInt(SAVED_SHOPPING_CENTER_LEVEL, 0);
        }

        // create the global data fragment and data the first time
        FragmentManager fm = getFragmentManager();
        GlobalDataFragment globalData = (GlobalDataFragment) fm.findFragmentByTag("data");
        if (globalData == null) {
            // add the fragment
            globalData = new GlobalDataFragment();
            fm.beginTransaction().add(globalData, "data").commit();
            ShoppingCenter.populateGlobalCenterList();
            GlobalDataFragment.currentCenter = new ShoppingCenter(getResources(), "Greenstone");
            GlobalDataFragment.offlineWifiScanner = null;
            GlobalDataFragment.wifiFingerprintInfo = new WifiFingerprintInfo(GlobalDataFragment.currentCenter.getConnectionPoints(),
                    GlobalDataFragment.currentCenter.getWifiFingerPrints(getResources()));
        }

        //Add floormap view
        setContentView(R.layout.activity_record);
        LinearLayout myLayout = (LinearLayout)findViewById(R.id.layout_canvas);

        floorMapView = new ScrollImageView(this, this);
        floorMapView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        myLayout.addView(floorMapView);

        // Set the available levels
        spinnerCurrentLevel = (Spinner)findViewById(R.id.spinner_current_level);
        spinnerCurrentLevel.setOnItemSelectedListener(this);
        String[] categoryArray = GlobalDataFragment.currentCenter.getLevels();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categoryArray);
        spinnerCurrentLevel.setAdapter(adapter);

        setLevel(level);

        floorMapView.setAutoScroll(mPrefs.getBoolean(PREF_AUTOSCROLL, true));
        floorMapView.setShowDebug(mPrefs.getBoolean(PREF_SHOWDEBUG, true));
        if (savedInstanceState!=null) {
            Bundle floorMapViewState = savedInstanceState.getBundle(SAVED_FLOORMAP_STATE);
            floorMapView.setState(floorMapViewState);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        if (GlobalDataFragment.locator==null) {
            Log.d("LOC", "no previous locator found, starting a new one");
            GlobalDataFragment.locator = new RecordForLocation(
                    GlobalDataFragment.currentCenter.getLocationParameters(
                            PreferenceManager.getDefaultSharedPreferences(this), getResources()),
                    this, new WifiScanner(wifiManager, GlobalDataFragment.currentCenter.getMacInputStream(getResources())));

        } else if (GlobalDataFragment.offlineWifiScanner!=null) {
                Log.d("LOC","offline scanner found, using that");
                GlobalDataFragment.locator.resetReferences(this, GlobalDataFragment.offlineWifiScanner);
        } else {
            Log.d("LOC","No locator started, because one is already running");
            GlobalDataFragment.locator.resetReferences(this,
                    new WifiScanner(wifiManager, GlobalDataFragment.currentCenter.getMacInputStream(getResources())));
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

        optionsMenu.findItem(R.id.menu_show_debug).setVisible(false);

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
        Bitmap floorMapImage = GlobalDataFragment.currentCenter.getImage(currentLevelID, getResources());

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density; // Later use this to get the scale image size.  Real pixels * density.
        Bitmap scaledFloormapImage = getResizedBitmap(floorMapImage, density);
        floorMapView.setImage(scaledFloormapImage, density, currentLevelID);
    }

    public Bitmap getResizedBitmap(Bitmap bm, float scale) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scale, scale);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }


    /**
     *
     * @param x actual pixel location of recording.  screen x needs to adjusted
     */
    public void makeRecording(final float x, final float y, final int level, final int delay) {
        String nStr = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_number_of_scans), "20");
        final int N = Integer.parseInt(nStr);
        final WifiStrengthRecorder wifiRecorder = new WifiStrengthRecorder(GlobalDataFragment.currentCenter.getPathName(), wifiManager, this);
        new Thread(new Runnable() {
            public void run() {
                wifiRecorder.MakeRecording(x, y, level, N, delay);
            }
        }).start();
    }

    public String getPref(int id){
        return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(id), null);
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
        GlobalDataFragment.locator = new RecordForLocation(
                GlobalDataFragment.currentCenter.getLocationParameters(
                        PreferenceManager.getDefaultSharedPreferences(this), getResources()),
                this, wifiScanner);
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
                ed.apply();
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
                ed.apply();
                return true;
            }
            case R.id.menu_clear_route: {
                GlobalDataFragment.latestRoute = null;
                floorMapView.invalidate();
                return true;
            }
            case R.id.menu_continuous_record: {
                Intent intent = new Intent(this, ContinuousRecordActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.menu_select_shopping_center: {
                Intent intent = new Intent(this, SelectCenterActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SELECT_CENTER);
                return true;
            }
            case R.id.menu_manual_record: {
                Intent intent = new Intent(this, ManualRecordActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.menu_test: {
                Intent intent = new Intent(this, LoadTestActivity.class);
                intent.putExtra("location", GlobalDataFragment.currentCenter.getPathName());
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
            String centerName = data.getStringExtra(LoadTestActivity.EXTRA_CENTER_NAME);
            runSimulatedPath(centerName, filename);
            return;
        }
        if (requestCode == REQUEST_CODE_SEARCH_SHOP && resultCode == RESULT_OK && data != null) {
            boolean directions = data.getBooleanExtra(SearchShopActivity.INTENT_EXTRA_DIRECTIONS, false);
            String category = data.getStringExtra(SearchShopActivity.INTENT_EXTRA_CATEGORY);
            String shopName = data.getStringExtra(SearchShopActivity.INTENT_EXTRA_SHOP);
            Log.d(TAG, "" + directions);
            Log.d(TAG, category);
            Log.d(TAG, shopName);
            Shop shop = GlobalDataFragment.currentCenter.getShopDirectory().getShop(category, shopName);
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

                GlobalDataFragment.latestRoute = GlobalDataFragment.currentCenter.getMallGraph().getRoute(floorMapView.getCurrentPosition(), shop);
                floorMapView.invalidate();
            }
        }
        if (requestCode == REQUEST_CODE_SELECT_CENTER && resultCode == RESULT_OK && data != null) {
            String selectedCenter = data.getStringExtra(SelectCenterActivity.INTENT_EXTRA_SELECTED_CENTER);
            setCurrentShoppingCenter(selectedCenter);
        }

    }

    /**
     * Replaces the wifi readings with readings from a file and starts locating
     */
    private void runSimulatedPath(String centerName, String pathFilename) {
        Log.d(TAG,"Getting summary macs");
        String macFilename = pathFilename.replace("path", "macs");
        floorMapView.updateMovementStatus("Getting summary macs");
        MacLookup summaryMacs = new MacLookup(GlobalDataFragment.currentCenter.getMacInputStream(getResources()));
        Log.d(TAG,"Getting path macs");
        floorMapView.updateMovementStatus("Getting path macs");
        MacLookup pathMacs = new MacLookup(centerName, macFilename);

        Log.d(TAG,"Processing path");
        floorMapView.updateMovementStatus("Processing path");
        GlobalDataFragment.offlineWifiScanner = new OfflineWifiScanner(pathFilename, centerName , summaryMacs, pathMacs,
                Calendar.getInstance().getTimeInMillis());

        Log.d(TAG,"Starting simulation");
        floorMapView.updateMovementStatus("Starting simulation");
        setCurrentShoppingCenter(centerName);
        startLocating(GlobalDataFragment.offlineWifiScanner);
    }


    /**
     * Sets the image and dots etc.  Only does this if the level changes
     * @param newLevelID - the new levelID
     */
    public void setLevel(int newLevelID)
    {
        if (newLevelID!=currentLevelID) {
            currentLevelID = newLevelID;
            updateFloorplan();

            floorMapView.setPreviousPoints(GlobalDataFragment.wifiFingerprintInfo.getXList(currentLevelID),
                    GlobalDataFragment.wifiFingerprintInfo.getYList(currentLevelID));
            floorMapView.invalidate();

            SharedPreferences.Editor ed = mPrefs.edit();
            ed.putInt(SAVED_SHOPPING_CENTER_LEVEL, currentLevelID);
            ed.apply();
        }
    }

    public void setCurrentShoppingCenter(String centerName) {
        GlobalDataFragment.currentCenter = new ShoppingCenter(getResources(), centerName);
        // TODO: If this ends up being slow do it on another thread.
        GlobalDataFragment.wifiFingerprintInfo = new WifiFingerprintInfo(
                GlobalDataFragment.currentCenter.getConnectionPoints(),
                GlobalDataFragment.currentCenter.getWifiFingerPrints(getResources()));

        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putString(SAVED_SHOPPING_CENTER_NAME, GlobalDataFragment.currentCenter.getPathName());
        ed.apply();
        setLevel(GlobalDataFragment.currentCenter.getDefaultLevel());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_SHOPPING_CENTER_NAME, GlobalDataFragment.currentCenter.getPathName());
        outState.putInt(SAVED_SHOPPING_CENTER_LEVEL, currentLevelID);
        outState.putBundle(SAVED_FLOORMAP_STATE, floorMapView.getState());
    }

    /**
     * This handles the button clicking on the submenus that pop up for various reasons.
     */
    @Override
    public void onOptionSet(Bundle results) {
        String type = results.getString("type");
        switch (type) {
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int level = GlobalDataFragment.currentCenter.getLevel(spinnerCurrentLevel.getSelectedItem().toString());
        setLevel(level);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
