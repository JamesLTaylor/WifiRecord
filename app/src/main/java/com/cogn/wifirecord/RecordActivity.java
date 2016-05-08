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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RecordActivity extends Activity
    implements PopupMenuDialogFragment.OptionSetListener,
        ScrollImageView.RecordMenuMaker,
        SharedPreferences.OnSharedPreferenceChangeListener  {

    private static final String TAG = "WIFI";
    public static String sessionStartTime = null;
    private PopupMenuDialogFragment popupMenu;
    private Menu optionsMenu;
    private ScrollImageView floorMapView;
    private Map<String, FloorPlanImageList> floorplans = new HashMap<>();
    static final String RO_RECORD = "Record Wifi at this Point";
    static final String RO_DELETE = "Delete this point";
    private ArrayList<String> recordOptions = new ArrayList<>(Arrays.asList(RO_RECORD, RO_DELETE));
    private String currentPlan;
    private String currentLevel;
    private WifiManager wifiManager;
    private static WifiStrengthRecorder wifiRecorder;
    private PreviousRecordings levelsAndPoints;
    private ReadingSummaryList summaryList;
    private RecordForLocation locator;
    private int currentLevelID;
    private ScrollImageView.ViewMode viewMode;

    // For movement detection
    private SensorManager sensorMan;
    private Sensor accelerometer;
    private ScrollImageView.ViewMode savedViewModeToUse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        floorplans.put("Greenstone", new FloorPlanImageList());
        floorplans.get("Greenstone").add(0, "Lower Level", R.drawable.greenstone_lower);
        floorplans.get("Greenstone").add(1, "Upper Level", R.drawable.greenstone_upper);

        floorplans.put("Home", new FloorPlanImageList());
        floorplans.get("Home").add(0, "Downstairs", R.drawable.house_lower);
        floorplans.get("Home").add(1, "Upstairs", R.drawable.house_upper);

        // Defaults:
        currentPlan = "Home";
        String levelDescriptionToUse = "Downstairs";
        savedViewModeToUse = ScrollImageView.ViewMode.RECORD;

        if (savedInstanceState!=null) {
            currentPlan = savedInstanceState.getString("currentPlan");
            levelDescriptionToUse = savedInstanceState.getString("currentLevel");
            savedViewModeToUse = (ScrollImageView.ViewMode)savedInstanceState.getSerializable("viewMode");
        }

        if (sessionStartTime==null) {
            Calendar c = Calendar.getInstance();
            sessionStartTime = DataReadWrite.timeStampFormat.format(c.getTime());
        }

        // WIFI Manager
        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        if (wifiRecorder == null) {
            wifiRecorder = new WifiStrengthRecorder(currentPlan, wifiManager, getBaseContext(), this);
        }
        else {
            wifiRecorder.SetActivity(this);
        }
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
            floorMapView.SetState(floorMapViewState);
        }
        levelsAndPoints = new PreviousRecordings(currentPlan);
        summaryList = new ReadingSummaryList(currentPlan);
        SetLevel(levelDescriptionToUse);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SetViewMode(savedViewModeToUse);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        if (viewMode==ScrollImageView.ViewMode.LOCATE)
        {
            // TODO: use the old locator with history.
            locator = new RecordForLocation(currentPlan, summaryList, this, wifiManager);
            sensorMan.registerListener(locator, accelerometer, SensorManager.SENSOR_DELAY_UI);
            locator.Start();
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

    public void UpdateRecordProgress(String newText) {
        floorMapView.UpdateRecordProgress(newText);
    }

    /**
     *
     * @return the id number of the current level
     */
    public int GetLevelID(){return currentLevelID;}

    /**
     * For use by the locator to run on the UI thread.
     * @param scores
     * @param bestGuessX
     * @param bestGuessY
     */
    public void UpdateLocateProgress(List<String> scores, float bestGuessX, float bestGuessY) {
        floorMapView.UpdateLocateProgress(scores, bestGuessX, bestGuessY);
    }

    public void SetScanFinished() {
        floorMapView.SetScanFinished();
    }

    private void UpdateFloorplan() {
        Bitmap floorMapImage = BitmapFactory.decodeResource(this.getResources(),
                floorplans.get(currentPlan).GetResource(currentLevel));

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density; // Later use this to get the scale image size.  Real pixels * density.
        floorMapView.setImage(floorMapImage, density);
    }


    /**
     *
     * @param x actual pixel location of recording.  screen x needs to adjusted
     * @param y
     * @param level
     * @param delay
     */
    public void makeRecording(final float x, final float y, final int level, final int delay) {
        String nStr = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_number_of_scans), "20");
        final int N = Integer.parseInt(nStr);
        new Thread(new Runnable() {
            public void run() {
                wifiRecorder.MakeRecording(x, y, level, N, delay);
            }
        }).start();
    }


    /**
     * Item selected in main action bar popupMenu
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.menu_locate: {
                // Start the locating thread
                SetViewMode(ScrollImageView.ViewMode.LOCATE);
                locator = new RecordForLocation(currentPlan, summaryList, this, wifiManager);
                sensorMan.registerListener(locator, accelerometer, SensorManager.SENSOR_DELAY_UI);
                locator.Start();
                return true;
            }
            case R.id.menu_record: {
                // Start the locating thread
                sensorMan.unregisterListener(locator);
                locator.Stop();
                SetViewMode(ScrollImageView.ViewMode.RECORD);
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
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void SetViewMode(ScrollImageView.ViewMode newMode){
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
        floorMapView.SetViewMode(viewMode);
    }


    /**
     * Sets the image and dots etc.
     * @param newLevelID
     */
    public void SetLevel(int newLevelID)
    {
        currentLevelID = newLevelID;
        currentLevel = floorplans.get(currentPlan).DescriptionFromID(newLevelID);
        UpdateFloorplan();

        floorMapView.SetPreviousPoints(
                levelsAndPoints.GetXList(currentLevelID), levelsAndPoints.GetYList(currentLevelID),
                summaryList.GetXList(currentLevelID), summaryList.GetYList(currentLevelID));
        floorMapView.invalidate();

    }

    /**
     * Gets the id and calls  {@link #SetLevel(int) SetLevel}
     * @param newLevelDescription
     */
    public void SetLevel(String newLevelDescription)
    {
        SetLevel(floorplans.get(currentPlan).IDFromDescription(newLevelDescription));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentPlan", currentPlan);
        outState.putString("currentLevel", currentLevel);
        outState.putSerializable("viewMode", viewMode);
        outState.putBundle("floorMapViewState", floorMapView.GetState());
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
                    currentPlan = results.getString("value");
                    wifiRecorder = new WifiStrengthRecorder(currentPlan, wifiManager, getBaseContext(), this);

                    // read the files that store previous points
                    // TODO: If this ends up being slow do it on another thread.
                    levelsAndPoints = new PreviousRecordings(currentPlan);
                    summaryList = new ReadingSummaryList(currentPlan);
                    SetLevel(floorplans.get(currentPlan).GetDefaultLevel());
                }
                popupMenu.dismiss();
                return;
            case "level":
                if (!currentLevel.equals(results.getString("value"))) {
                    SetLevel(results.getString("value"));
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
                        floorMapView.DeletePoint();
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
    public void MakeRecordMenu(float x, float y) {
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
