package com.cogn.wifirecord;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
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

public class ContinuousRecordActivity extends Activity{

    private static final String TAG = "CONTINUOUS RECORD";
    private long startTimeMillis;
    private int counter;
    private boolean scanRunning;
    private WifiManager wifiManager;
    private MacLookup macLookup;
    private String location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_continuous_record);
        Intent myIntent = getIntent();
        location = myIntent.getStringExtra("location");
        wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        macLookup = new MacLookup(location);
        Start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }



    public void UpdateResults(String value)
    {
        TextView view = (TextView)findViewById(R.id.continuous_update_info);
        view.setText(value);
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
        String scanStartTime = DataReadWrite.timeStampFormat.format(c.getTime());
        // Make the file and folders
        File folder = new File(Environment.getExternalStorageDirectory(), "WifiRecord/"+location);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, location.toLowerCase().trim() + "_continuous_" + scanStartTime +".txt");
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
    }

}
