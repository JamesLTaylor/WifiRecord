package com.cogn.wifirecord;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadFactory;

public class WifiStrengthRecorder {
    private static final String ACTION_RESULTS_WRITTEN = "com.cogn.wifirecord.RESULTS_WRITTEN";
    private static final String TAG = "WIFI";
    private File file;
    private WifiManager wifiManager;
    private MacLookup macLookup;


    private float x;
    private float y;
    private int level;
    private int scanCounter;
    private int N;
    private long startTimeMillis;
    private long scanBase = 0;


    public WifiStrengthRecorder(String location, WifiManager wifiManager, Context context)
    {
        this.wifiManager = wifiManager;
        macLookup = new MacLookup(location);
        File folder = new File(Environment.getExternalStorageDirectory(), "WifiRecord");
        file = new File(folder, location.toLowerCase().trim() + "_readings.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ioe)
            {
                Log.e(TAG, "could not make file", ioe);
                return;
            }
        }
        //IntentFilter intentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        //WifiScanReceiver receiver = new WifiScanReceiver();
        //context.registerReceiver(receiver, intentFilter);
    }

    @Override
    public void finalize()
    {
        try {
            super.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void MakeRecording(float x, float y, int level, int N, int delay) {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date startTime = c.getTime();
        startTimeMillis = c.getTimeInMillis();
        String formattedDate = df.format(startTime);
        Integer macID;
        try {
            BufferedWriter filewriter = new BufferedWriter(new FileWriter(file, true));
            Log.d(TAG, "NEW" + "," + String.format("%.1f", x) + "," + String.format("%.1f", y) + "," + level + "," + formattedDate + "\n");
            filewriter.write("NEW" + "," + String.format("%.1f", x) + "," + String.format("%.1f", y) + "," + level + "," + formattedDate + "\n");

            int counter = 0;
            long offset;
            ArrayList<Integer> oldScanned = null;
            List<ScanResult> scanned;
            while (counter<N){
                scanned = wifiManager.getScanResults();
                if (HaveChanged(oldScanned, scanned)) {
                    offset = Calendar.getInstance().getTimeInMillis() - startTimeMillis;
                    Log.d(TAG, "OFFSET," + offset+"\n");
                    filewriter.write("OFFSET," + offset+"\n");
                    oldScanned = new ArrayList<Integer>();
                    for (ScanResult scan : scanned) {
                        macID = macLookup.GetId(scan.BSSID, scan.SSID);
                        Log.d(TAG, macID + "," + scan.level+"\n");
                        filewriter.write(macID + "," + scan.level+"\n");
                        oldScanned.add(scan.level);
                    }
                    counter++;
                }
                try { Thread.sleep(100); }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                wifiManager.startScan();
            }
            filewriter.close();
        } catch (IOException e) {
            Log.e(TAG, "could not make file", e);
            return;
        }
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


    public class WifiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scanned = wifiManager.getScanResults();
            long offset = Calendar.getInstance().getTimeInMillis() - startTimeMillis;
            Integer macID;

            BufferedWriter filewriter = null;
            try {
                filewriter = new BufferedWriter(new FileWriter(file, true));
                Log.d(TAG, "OFFSET," + offset+"\n");
                filewriter.write("OFFSET," + offset+"\n");
                for (ScanResult scan : scanned) {
                    if (scanBase == 0) scanBase = scan.timestamp;
                    macID = macLookup.GetId(scan.BSSID, scan.SSID);
                    Log.d(TAG, macID + "," + scan.level+"\n");
                    Log.d(TAG, "age of scan" + (scan.timestamp - scanBase));
                    filewriter.write(macID + "," + scan.level+"\n");
                }
                filewriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            scanCounter++;
            Log.d(TAG, "RESULTS RECEIVED, SCAN AGAIN!");
            Log.d(TAG, "" + scanCounter);

            Intent i = new Intent(ACTION_RESULTS_WRITTEN);
            context.sendBroadcast(i);
        }
    }

    public class ResultsDoneReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "CUSTOM EVENT RECEIVED");
            if (scanCounter<N) {
                WifiManager w = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                boolean result = w.startScan();
                Log.d(TAG, ""+result);
            }
        }
    }
}
