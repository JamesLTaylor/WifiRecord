package com.cogn.wifirecord;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.FloatMath;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static android.util.FloatMath.sqrt;


public class RecordActivity extends Activity
    implements PopupMenuDialogFragment.OptionSetListener {

    private static final String TAG = "WIFI";
    private PopupMenuDialogFragment menu;
    private ScrollImageView floorMapView;

    private class FloorPlanImageList{
        private List<Integer> ids = new ArrayList<Integer>();
        public ArrayList<String> descriptions = new ArrayList<String>();
        private List<Integer> resourceIDs = new ArrayList<Integer>();
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
        public String GetDefault()
        {
            return descriptions.get(0);
        }
    }

    private Map<String, FloorPlanImageList> floorplans = new HashMap<String, FloorPlanImageList>();

    static final String RO_RECORD = "Record Wifi at this Point";
    static final String RO_DELETE = "Delete this point";
    private ArrayList<String> recordOptions = new ArrayList<String>(Arrays.asList(RO_RECORD, RO_DELETE));
    private String currentPlan;
    private String currentLevel;
    private WifiManager wifiManager;
    private WifiStrengthRecorder wifiRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentPlan = "Greenstone";
        currentLevel = "Lower Level";

        if (savedInstanceState==null) {
            currentPlan = "Greenstone";
            currentLevel = "Lower Level";
        }
        else {
            currentPlan = savedInstanceState.getString("currentPlan");
            currentLevel = savedInstanceState.getString("currentLevel");
        }

        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        wifiRecorder = new WifiStrengthRecorder(currentPlan, wifiManager, getBaseContext());

        floorplans.put("Greenstone", new FloorPlanImageList());
        floorplans.get("Greenstone").add(0, "Lower Level", R.drawable.greenstone_lower);
        floorplans.get("Greenstone").add(1, "Upper Level", R.drawable.greenstone_upper);

        floorplans.put("Home", new FloorPlanImageList());
        floorplans.get("Home").add(0, "Downstairs", R.drawable.house_lower);
        floorplans.get("Home").add(1, "Upstairs", R.drawable.house_upper);

        setContentView(R.layout.activity_record);
        LinearLayout myLayout = (LinearLayout)findViewById(R.id.recordLayout);

        floorMapView = new ScrollImageView(this);

        floorMapView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        UpdateFloorplan();
        floorMapView.setOnTouchListener(planTouchListener);
        myLayout.addView(floorMapView);

    }

    private OnTouchListener planTouchListener = new OnTouchListener() {
        private long startClickTime;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            ScrollImageView imV = (ScrollImageView)view;
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                startClickTime = Calendar.getInstance().getTimeInMillis();
                imV.mCurrentX = event.getX();
                imV.mCurrentY = event.getY();
            }
            else if (event.getAction()==MotionEvent.ACTION_UP){
                long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                float imageX = imV.mCurrentX-imV.mTotalX;
                float imageY = imV.mCurrentY-imV.mTotalY;
                if(clickDuration < 200) {
                    //click event has occurred
                    boolean addCircle = true;
                    if (imV.latestCircleX!=null && imV.latestCircleY!=null) {
                        float dist = sqrt(FloatMath.pow(imV.latestCircleX - imageX, 2) + FloatMath.pow(imV.latestCircleY - imageY, 2));
                        if (dist < 50.0) {
                            menu = new PopupMenuDialogFragment();
                            Bundle bundle = new Bundle();
                            bundle.putStringArrayList("options", recordOptions);
                            bundle.putString("type", "record");
                            bundle.putFloat("x", imV.latestCircleX);
                            bundle.putFloat("y", imV.latestCircleY);
                            menu.setArguments(bundle);
                            menu.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
                            menu.show(getFragmentManager(), "menu");
                            addCircle = false;
                        }
                    }
                    if (addCircle) {
                        imV.latestCircleX = imageX;
                        imV.latestCircleY = imageY;
                        imV.circleX.add(imageX);
                        imV.circleY.add(imageY);
                    }
                }
                imV.invalidate();
            }
            else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float x = event.getX();
                float y = event.getY();

                // Update how much the touch moved
                imV.mDeltaX = x - imV.mCurrentX;
                imV.mDeltaY = y - imV.mCurrentY;

                imV.mCurrentX = x;
                imV.mCurrentY = y;
                imV.invalidate();
            }
            // Consume event
            return true;
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.record, menu);
        return true;
    }

    private void UpdateFloorplan() {
        Bitmap floorMapImage = BitmapFactory.decodeResource(this.getResources(),
                floorplans.get(currentPlan).GetResource(currentLevel));
        floorMapView.setImage(floorMapImage);
        floorMapView.invalidate();
    }

    public void makeRecording(float x, float y, int level, int N, int delay) {
        wifiRecorder.MakeRecording(x, y, level, N, delay);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.menu_select_location: {
                View view = findViewById(R.id.recordLayout);
                menu = new PopupMenuDialogFragment();
                Bundle options = new Bundle();
                options.putStringArrayList("options", new ArrayList<String>(floorplans.keySet()));
                options.putString("type", "location");
                menu.setArguments(options);
                menu.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
                menu.show(getFragmentManager(), "menu");
                return true;
            }
            case R.id.menu_select_level: {
                View view = findViewById(R.id.recordLayout);
                menu = new PopupMenuDialogFragment();
                Bundle options = new Bundle();
                options.putStringArrayList("options", floorplans.get(currentPlan).descriptions);
                options.putString("type", "level");
                menu.setArguments(options);
                menu.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
                menu.show(getFragmentManager(), "menu");
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentPlan", currentPlan);
        outState.putString("currentLevel", currentLevel);
    }

    @Override
    public void onOptionSet(Bundle results) {
        String type = results.getString("type");
        switch (type) {
            case "location":
                currentPlan = results.getString("value");
                currentLevel = floorplans.get(currentPlan).GetDefault();
                wifiRecorder = new WifiStrengthRecorder(currentPlan, wifiManager, getBaseContext());
                UpdateFloorplan();
                menu.dismiss();
                return;
            case "level":
                currentLevel = results.getString("value");
                UpdateFloorplan();
                menu.dismiss();
                return;
            case "record":
                String value = results.getString("value");
                switch (value) {
                    case RO_RECORD:
                        makeRecording(results.getFloat("x"), results.getFloat("y"), 0, 10, 500);
                        break;
                    case RO_DELETE:
                        break;
                    default:
                        throw new IllegalArgumentException("Not a known menu option: " + value);
                }
                menu.dismiss();
                return;

        }
    }
}
