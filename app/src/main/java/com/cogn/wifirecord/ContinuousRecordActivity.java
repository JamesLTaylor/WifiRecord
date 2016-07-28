package com.cogn.wifirecord;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ContinuousRecordActivity extends Activity
        implements DialogInterface.OnDismissListener, DialogInterface.OnKeyListener,
        DialogInterface.OnCancelListener, View.OnClickListener
{

    private static final String TAG = "CONTINUOUS RECORD";
    private long startTimeMillis;
    private int counter;
    private static boolean scanRunning;
    private WifiManager wifiManager;
    private MacLookup macLookup;
    private String location;
    private String deviceName;

    private static String filename;
    private static String macName;
    private String description;
    private Spinner pathName;
    private Switch forwardBackwardSwitch;
    private static TextView textView;
    private static boolean requestStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_continuous_record);
        location = GlobalDataFragment.currentCenter.getPathName();
        pathName = (Spinner)findViewById(R.id.continuous_record_path_name);
        forwardBackwardSwitch = (Switch)findViewById(R.id.continuous_record_direction);
        textView = (TextView)findViewById(R.id.continuous_record_info);


        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        deviceName = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_general_device_name), "");
        if (deviceName.length() < 4) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton("OK", null);
            builder.setTitle("Error");
            builder.setMessage("Device name not set.  Please set a name of at least 3 characters under Settings->General");
            builder.setOnDismissListener(this);
            builder.setOnKeyListener(this);
            builder.setOnCancelListener(this);
            builder.show();
            //finish();
            return;
        }

        //TODO: Store these strings in a text file on the device
        String[] DESCRIPTIONS = getPathDescriptionsFromFile();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.spinner_item_shops, DESCRIPTIONS);
        Spinner spinner = (Spinner) findViewById(R.id.continuous_record_path_name);
        spinner.setAdapter(adapter);

        findViewById(R.id.btn_continuous_record_start).setOnClickListener(this);
        findViewById(R.id.btn_continuous_record_stop).setOnClickListener(this);

        if (scanRunning) {
            //LinearLayout layout = (LinearLayout) findViewById(R.id.continuous_record_layout);
            //View view = findViewById(R.id.continuous_record_description);
            //layout.removeView(view);
            boolean forwardBackwardState = savedInstanceState.getBoolean("forwardBackwardState");
            forwardBackwardSwitch.setSelected(forwardBackwardState);
            int pathNamePostion = savedInstanceState.getInt("pathNamePostion");
            pathName.setSelection(pathNamePostion);
            pathName.setEnabled(false);
            forwardBackwardSwitch.setEnabled(false);
            findViewById(R.id.btn_continuous_record_start).setEnabled(false);
            findViewById(R.id.btn_continuous_record_stop).setEnabled(true);

        } else if (savedInstanceState!=null) {
            boolean forwardBackwardState = savedInstanceState.getBoolean("forwardBackwardState");
            forwardBackwardSwitch.setSelected(forwardBackwardState);
            int pathNamePostion = savedInstanceState.getInt("pathNamePostion");
            pathName.setSelection(pathNamePostion);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            stop();
        } else {
            //It's an orientation change.
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("forwardBackwardState", forwardBackwardSwitch.isChecked());
        outState.putInt("pathNamePostion", pathName.getSelectedItemPosition());
        outState.putString("filename", filename);
        outState.putString("macName", macName);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }



    public void updateResults(String value)
    {
        textView.setText(value);
    }

    private void writeToUIThread(final String newText)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateResults(newText);
            }
        });
    }

    public void stop() {
        requestStop = true;
        Toast.makeText(this, "SCAN STOP REQUESTED. WAITING...", Toast.LENGTH_SHORT);
        for (int i = 0; i < 30; i++) {
            if (!scanRunning) {
                Toast.makeText(this, "CONFIRMED SCAN STOP", Toast.LENGTH_SHORT);
                requestStop = false;
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Toast.makeText(this, "\"Scan did not stop after 3s, something is wrong.  request stop flag left on.", Toast.LENGTH_SHORT);
        scanRunning = false;
    }

    public void start() {
        scanRunning = true;
        Log.d(TAG, "SCAN STARTED");
        new Thread(new Runnable() {
            public void run() {
                startScanning();
            }
        }).start();
    }

    private boolean haveChanged(ArrayList<Integer> oldScanned, List<ScanResult> scanned) {
        if (oldScanned==null) return true;
        if (oldScanned.size()!=scanned.size()) return true;
        for (int i = 0; i<oldScanned.size(); i++)
        {
            if (oldScanned.get(i)!=scanned.get(i).level) return true;
        }
        return false;
    }

    private void startScanning() {
        Calendar c = Calendar.getInstance();
        // Make the file and folders
        File folder = new File(Environment.getExternalStorageDirectory(), "WifiRecord/"+location);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, filename);
        try {
            file.createNewFile();
            BufferedWriter filewriter = new BufferedWriter(new FileWriter(file, true));
            filewriter.write("DEVICE," + android.os.Build.BRAND + "," + android.os.Build.MODEL + "\n");
            String direction = forwardBackwardSwitch.isChecked()?"1":"-1";
            filewriter.write("DESCRIPTION," + description + "\n");
            filewriter.write("DIRECTION," + direction + "\n");
            filewriter.close();

        } catch (IOException ioe)
        {
            Log.e(TAG, "could not make file", ioe);
            return;
        }

        // Start recording

        ArrayList<Integer> oldScanned = null;
        List<ScanResult> scanned;
        long offset;
        int macID;
        scanRunning = true;
        startTimeMillis = c.getTimeInMillis();


        while (!requestStop){
            scanned = wifiManager.getScanResults();
            if (haveChanged(oldScanned, scanned)) {
                try {
                    String results = "";
                    BufferedWriter filewriter = new BufferedWriter(new FileWriter(file, true));
                    offset = Calendar.getInstance().getTimeInMillis() - startTimeMillis;
                    Log.d(TAG, "OFFSET," + offset + "\n");
                    filewriter.write("OFFSET," + offset + "\n");
                    results+="OFFSET," + offset + "\n";
                    oldScanned = new ArrayList<Integer>();
                    for (ScanResult scan : scanned) {
                        macID = macLookup.getId(scan.BSSID, scan.SSID);
                        Log.d(TAG, macID + "," + scan.level + "\n");
                        filewriter.write(macID + "," + scan.level + "\n");
                        results+=""  + macID + "," + scan.level + "\n";
                        oldScanned.add(scan.level);
                    }
                    writeToUIThread(results);
                    filewriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                long delayMS = 100;
                Thread.sleep(delayMS); }
            catch (InterruptedException e) {
                e.printStackTrace();
                scanRunning = false;
            }
            wifiManager.startScan();
        }
        scanRunning = false;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        dialog.dismiss();
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId()==R.id.btn_continuous_record_start) {
            description = ((Spinner) findViewById(R.id.continuous_record_path_name)).getSelectedItem().toString().toUpperCase();
            findViewById(R.id.continuous_record_path_name).setEnabled(false);
            findViewById(R.id.continuous_record_direction).setEnabled(false);
            findViewById(R.id.btn_continuous_record_start).setEnabled(false);
            findViewById(R.id.btn_continuous_record_stop).setEnabled(true);
            //LinearLayout layout = (LinearLayout) findViewById(R.id.continuous_record_layout);
            //View view = findViewById(R.id.continuous_record_description);
            //layout.removeView(view);

            String scanStartTime = DataReadWrite.timeStampFormat.format(Calendar.getInstance().getTime());
            filename = location.toLowerCase().trim() + "_" + scanStartTime + "_" + deviceName + "_path.txt";
            macName = location.toLowerCase().trim() + "_" + scanStartTime + "_" + deviceName + "_macs.txt";
            macLookup = new MacLookup(location, macName);
            start();
        } else if (v.getId()==R.id.btn_continuous_record_stop) {
            findViewById(R.id.continuous_record_path_name).setEnabled(true);
            findViewById(R.id.continuous_record_direction).setEnabled(true);
            findViewById(R.id.btn_continuous_record_start).setEnabled(true);
            findViewById(R.id.btn_continuous_record_stop).setEnabled(false);
            stop();
        }
    }

    private String[] getPathDescriptionsFromFile() {
        List<String> descriptionList = new ArrayList<>();
        InputStream inputStream = GlobalDataFragment.currentCenter.getPathDescriptions(getResources());
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String str;
            while ((str = in.readLine()) != null) {
                descriptionList.add(str);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] returnArray = new String[descriptionList.size()];
        return descriptionList.toArray(returnArray);
    }
}
