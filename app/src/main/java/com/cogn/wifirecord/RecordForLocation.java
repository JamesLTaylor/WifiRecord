package com.cogn.wifirecord;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by James on 5/6/2016.
 */
public class RecordForLocation {
    private static final String TAG = "WIFI_LOCATE";
    private final String location;
    private final ReadingSummaryList summaryList;
    private RecordActivity callingActivity;
    private WifiManager wifiManager;
    private MacLookup macLookup;
    boolean scanRunning = false;

    public RecordForLocation(String location, ReadingSummaryList summaryList,
                             RecordActivity callingActivity, WifiManager wifiManager) {
        this.location = location;
        this.summaryList = summaryList;
        this.callingActivity = callingActivity;
        this.wifiManager = wifiManager;
        macLookup = new MacLookup(location);
    }


    public void Stop() {
        scanRunning = false;
        Log.d(TAG, "SCAN STOPPED");
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

    public void StartScanning(){
        ArrayList<Integer> oldScanned = null;
        List<ScanResult> scanned;
        long startTimeMillis = Calendar.getInstance().getTimeInMillis();
        RecentScanQueue recentScans = new RecentScanQueue(startTimeMillis);

        long offset;
        int macID;
        while (scanRunning){
            //UpdateProgressOnUIThread("" + (counter+1) + " of " + N);
            scanned = wifiManager.getScanResults();
            if (HaveChanged(oldScanned, scanned)) {
                offset = Calendar.getInstance().getTimeInMillis() - startTimeMillis;
                recentScans.AddNew(offset);
                Log.d(TAG, "OFFSET," + offset+"\n");
                oldScanned = new ArrayList<Integer>();
                for (ScanResult scan : scanned) {
                    macID = macLookup.GetId(scan.BSSID, scan.SSID);
                    Log.d(TAG, macID + "," + scan.level+"\n");
                    recentScans.UpdateEnd(macID, (float)scan.level);
                    oldScanned.add(scan.level);
                }
                HashMap<Integer, List<Float>> summary = recentScans.GetSummary();
                int levelID = callingActivity.GetLevelID();
                List<String> scores = summaryList.GetScores(summary, levelID);
                UpdateOnUIThread(scores);
            }
            try { Thread.sleep(100); }
            catch (InterruptedException e) {
                e.printStackTrace();
                scanRunning = false;
            }
            wifiManager.startScan();
        }
    }

    private void UpdateOnUIThread(final List<String> scores)
    {
        callingActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callingActivity.UpdateLocateProgress(scores);
            }
        });
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

    private class RecentScanQueue{
        ArrayDeque<Long> times;
        ArrayDeque<Map<Integer, Float>> values;
        long latestTime;
        long maxTime = 5000;

        public RecentScanQueue(long latestTime)
        {
            this.latestTime = latestTime;
            times = new ArrayDeque<>();
            values = new ArrayDeque<>();
        }

        /**
         * Adds a new empty record to the end of the queue and removes any records from the start
         * that are too far in the past.
         * @param latestTime
         */
        public void AddNew(long latestTime)
        {
            this.latestTime = latestTime;
            times.addLast(latestTime);
            values.addLast(new HashMap<Integer, Float>());
            if (times.size()>0) {
                while (times.peekFirst() < latestTime - maxTime) {
                    times.removeFirst();
                    values.removeFirst();
                }
            }
        }

        public void UpdateEnd(Integer macID, Float reading)
        {
            values.peekLast().put(macID, reading);
        }

        private float GetMean(List<Float> values) {
            double total = 0.0;
            for (Float value : values)
                total += value;
            return (float)(total/values.size());
        }

        /**
         * Population standard deviation in case there is only one entry point
         * @param values
         * @return
         */
        private float GetStd(List<Float> values) {
            double total = 0.0;
            float mean = GetMean(values);
            for (Float value : values)
                total += (value-mean)*(value-mean);
            return (float)Math.sqrt(total/values.size());
        }


        public HashMap<Integer, List<Float>> GetSummary() {
            HashMap<Integer, ArrayList<Float>> aggregate = new HashMap<Integer, ArrayList<Float>>();
            for (Map<Integer, Float> value : values)
            {
                for (Map.Entry<Integer, Float> entry : value.entrySet()) {
                    if (!aggregate.containsKey(entry.getKey())) {
                        aggregate.put(entry.getKey(), new ArrayList<Float>(Arrays.asList(entry.getValue())));
                    }
                    else {
                        aggregate.get(entry.getKey()).add(entry.getValue());
                    }
                }
            }
            HashMap<Integer, List<Float>> summary = new HashMap<Integer, List<Float>>();
            float p;
            float mu;
            float sigma;
            for (Map.Entry<Integer, ArrayList<Float>> aggregateEntry : aggregate.entrySet()) {
                p = aggregateEntry.getValue().size()/values.size();
                mu = GetMean(aggregateEntry.getValue());
                sigma = GetStd(aggregateEntry.getValue());
                summary.put(aggregateEntry.getKey(), new ArrayList<Float>(Arrays.asList(p, mu, sigma)));
            }
            return summary;
        }
    }
}
