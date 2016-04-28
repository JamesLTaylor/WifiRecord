package com.cogn.wifirecord;

import android.app.Activity;
import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.util.FloatMath.sqrt;


public class RecordActivity extends Activity
    implements PopupMenuDialogFragment.OptionSetListener {

    private PopupMenuDialogFragment menu;
    private ScrollImageView floorMapView;

    private Map<String, Map<String, Integer>> floorplans = new HashMap<String, Map<String, Integer>>();

    static final String RO_RECORD = "Record Wifi at this Point";
    static final String RO_DELETE = "Delete this point";
    private ArrayList<String> recordOptions = new ArrayList<String>(Arrays.asList(RO_RECORD, RO_DELETE));
    private String currentPlan;
    private String currentLevel;

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

        floorplans.put("Greenstone", new HashMap<String, Integer>());
        floorplans.get("Greenstone").put("Upper Level", R.drawable.greenstone_upper);
        floorplans.get("Greenstone").put("Lower Level", R.drawable.greenstone_lower);
        floorplans.put("Home", new HashMap<String, Integer>());
        floorplans.get("Home").put("Upstairs", R.drawable.house_upper);
        floorplans.get("Home").put("Downstairs", R.drawable.house_lower);

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
                        if (dist < 20.0) {
                            menu = new PopupMenuDialogFragment();
                            Bundle bundle = new Bundle();
                            bundle.putStringArrayList("options", recordOptions);
                            bundle.putString("type", "record");
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
                floorplans.get(currentPlan).get(currentLevel));
        floorMapView.setImage(floorMapImage);
        floorMapView.invalidate();
    }

    public void makeRecording() {
        /*TextView results = (TextView)findViewById(R.id.recordResults);
        results.append("\n\nscanning...\n");

        WifiManager manager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        manager.startScan();
        List<ScanResult> scanned = manager.getScanResults();
        for (ScanResult scan : scanned) {
            results.append(scan.SSID+"\n");
            results.append(scan.BSSID+"\n");
            results.append(scan.level+"\n");
        }*/
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.menu_select_location:
                View view = findViewById(R.id.recordLayout);
                menu = new PopupMenuDialogFragment();
                Bundle options = new Bundle();
                options.putStringArrayList("options", new ArrayList<String>(floorplans.keySet()));
                options.putString("type", "location");
                menu.setArguments(options);
                menu.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
                menu.show(getFragmentManager(), "menu");

                return true;
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
                Iterator currentLevelIter = floorplans.get(currentPlan).keySet().iterator();
                currentLevel = (String)currentLevelIter.next();
                UpdateFloorplan();
                menu.dismiss();
            case "record":
                String value = results.getString("value");
                switch (value) {
                    case RO_RECORD:
                        makeRecording();
                        return;
                    case RO_DELETE:
                        return;
                    default:
                        throw new IllegalArgumentException("Not a known menu option: " + value);
                }

        }
    }
}
