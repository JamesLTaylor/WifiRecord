package com.cogn.wifirecord;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ContinuousRecordActivity extends Activity
        implements DialogInterface.OnDismissListener, DialogInterface.OnKeyListener, DialogInterface.OnCancelListener
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
    private static TextView textView;
    private static String macName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_continuous_record);
        textView = (TextView)findViewById(R.id.continuous_update_info);
        Intent myIntent = getIntent();
        location = myIntent.getStringExtra("location");
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
        if (savedInstanceState==null) {
            String scanStartTime = DataReadWrite.timeStampFormat.format(Calendar.getInstance().getTime());
            filename = location.toLowerCase().trim() + "_path_"+deviceName+"_" + scanStartTime +".txt";
            macName = location.toLowerCase().trim() + "_macs_"+deviceName+"_" + scanStartTime +".txt";
            macLookup = new MacLookup(location, macName);
            Start();
        } else {
            macName = savedInstanceState.getString("macName");
            filename = savedInstanceState.getString("filename");
            macLookup = new MacLookup(location, macName);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            Stop();
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
        outState.putString("filename", filename);
        outState.putString("macName", macName);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }



    public void UpdateResults(String value)
    {
        textView.setText(value);
    }

    private void WriteToUIThread(final String newText)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                UpdateResults(newText);
            }
        });
    }

    public void Stop() {
        scanRunning = false;
    }

    public void Start() {
        scanRunning = true;
        Log.d(TAG, "SCAN STARTED");
        new Thread(new Runnable() {
            public void run() {
                StartScanning();
            }
        }).start();
    }

    private boolean HaveChanged(ArrayList<Integer> oldScanned, List<ScanResult> scanned) {
        if (oldScanned==null) return true;
        if (oldScanned.size()!=scanned.size()) return true;
        for (int i = 0; i<oldScanned.size(); i++)
        {
            if (oldScanned.get(i)!=scanned.get(i).level) return true;
        }
        return false;
    }

    private void StartScanning() {
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


        while (scanRunning){
            scanned = wifiManager.getScanResults();
            if (HaveChanged(oldScanned, scanned)) {
                try {
                    String results = "";
                    BufferedWriter filewriter = new BufferedWriter(new FileWriter(file, true));
                    offset = Calendar.getInstance().getTimeInMillis() - startTimeMillis;
                    Log.d(TAG, "OFFSET," + offset + "\n");
                    filewriter.write("OFFSET," + offset + "\n");
                    results+="OFFSET," + offset + "\n";
                    oldScanned = new ArrayList<Integer>();
                    for (ScanResult scan : scanned) {
                        macID = macLookup.GetId(scan.BSSID, scan.SSID);
                        Log.d(TAG, macID + "," + scan.level + "\n");
                        filewriter.write(macID + "," + scan.level + "\n");
                        results+=""  + macID + "," + scan.level + "\n";
                        oldScanned.add(scan.level);
                    }
                    WriteToUIThread(results);
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
        // Scan finished.  Take a copy of the macs.

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
}
