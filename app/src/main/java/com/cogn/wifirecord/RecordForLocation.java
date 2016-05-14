package com.cogn.wifirecord;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.FloatMath;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to estimate location of user based on Wifi readings.
 */
public class RecordForLocation implements SensorEventListener {
    private static final String TAG = "WIFI_LOCATE";
    private final String location;
    private final ReadingSummaryList summaryList;
    private RecordActivity callingActivity;
    private WifiManager wifiManager;
    private MacLookup macLookup;
    private ConnectionPoints connectionPoints;
    private int nearestConnectionIndex;
    private float pxPerM;
    private boolean scanRunning = false;
    private long delayMS = 100;

    //For motion sensor
    private float[] mGravity;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;
    private boolean resetSinceMoveQueue;

    //For location
    private long offset;
    private float currentX; // where the circle is currently drawn
    private float currentY;
    private float bestFitX; // the current best guess place
    private float bestFitY;
    private int bestFitLevel;
    private int bestFitIndex;
    private long bestFitTime;
    private ReadingsQueue m_shortQueue;
    private ReadingsQueue m_sinceMoveQueue;

    // Values for drifting circle
    private long prevTime = 0;
    private float dx = 0;
    private float dy = 0;
    private float walking_pace =  2.0f; // m/s FAST: 7.6km/h;
    private float pxPerDelay;

    public RecordForLocation(String location, ConnectionPoints connectionPoints, float pxPerM,
                             ReadingSummaryList summaryList,
                             RecordActivity callingActivity, WifiManager wifiManager) {
        this.location = location;
        this.connectionPoints = connectionPoints;
        this.pxPerM = pxPerM;
        pxPerDelay = walking_pace*pxPerM * delayMS/1000;
        this.summaryList = summaryList;
        this.callingActivity = callingActivity;
        this.wifiManager = wifiManager;
        macLookup = new MacLookup(location);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
    }

    public void Stop() {
        scanRunning = false;
        //Log.d(TAG, "SCAN STOPPED");
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

    private void UpdateMarkedLocation(boolean checkDirection)
    {
        if (prevTime==0){
            prevTime = offset;
            currentX = bestFitX;
            currentY = bestFitY;
            return;
        }
        if (checkDirection) {
            if (Math.abs(bestFitX -currentX)<pxPerDelay/2 && Math.abs(bestFitY -currentY)<pxPerDelay/2) {
                currentX = bestFitX;
                currentY = bestFitY;
                dx = 0;
                dy = 0;
            } else {
                double theta = Math.atan((bestFitY -currentY)/(bestFitX -currentX));
                if ((bestFitX -currentX)<0) theta+= Math.PI;
                //TODO: check if I need to handle theta = +-pi/2
                dx = pxPerDelay*(float)Math.cos(theta);
                dy = pxPerDelay*(float)Math.sin(theta);
            }
        } else {
            if (Math.abs(bestFitX - currentX) < pxPerDelay / 2 && Math.abs(bestFitY - currentY) < pxPerDelay / 2) {
                currentX = bestFitX;
                currentY = bestFitY;
                dx = 0;
                dy = 0;
            } else {
                currentX += dx;
                currentY += dy;
            }
        }
    }



    public void StartScanning(){
        resetSinceMoveQueue = false;
        ArrayList<Integer> oldScanned = null;
        List<ScanResult> scanned;
        long startTimeMillis = Calendar.getInstance().getTimeInMillis();
        m_shortQueue = new ReadingsQueue(3);
        m_sinceMoveQueue = new ReadingsQueue(20);
        int macID;
        List<String> scores = null;
        bestFitIndex = -1;

        while (scanRunning){
            scanned = wifiManager.getScanResults();
            if (HaveChanged(oldScanned, scanned)) {
                offset = Calendar.getInstance().getTimeInMillis() - startTimeMillis;

                // Add the scan to the Queues
                m_shortQueue.AddNew(offset);
                if (resetSinceMoveQueue) {
                    m_sinceMoveQueue.clear();
                    resetSinceMoveQueue = false;
                    //Log.d(TAG, "Reset sinceMoveQueue");
                }
                m_sinceMoveQueue.AddNew(offset);
                //Log.d(TAG, "OFFSET," + offset+"\n");
                oldScanned = new ArrayList<Integer>();
                for (ScanResult scan : scanned) {
                    macID = macLookup.GetId(scan.BSSID, scan.SSID);
                    //Log.d(TAG, macID + "," + scan.level+"\n");
                    m_shortQueue.UpdateEnd(macID, (float)scan.level);
                    m_sinceMoveQueue.UpdateEnd(macID, (float)scan.level);
                    oldScanned.add(scan.level);
                }

                // Find the best fit location, sets bestFitX and bestFitY
                UpdateBestFit();
                // Check which direction the bestGuess should move
                UpdateMarkedLocation(true);
                // Get the scores to display on the floorMap
                scores = summaryList.GetScores(callingActivity.GetLevelID()).scores;
            } else {
                UpdateMarkedLocation(false); // No new reading, just drift the circle if required.
            }

            if (scores!=null) UpdateOnUIThread(scores, currentX, currentY);

            try { Thread.sleep(delayMS); }
            catch (InterruptedException e) {
                e.printStackTrace();
                scanRunning = false;
            }
            wifiManager.startScan();
        }
    }

    private void UpdateBestFit() {
        float thresh1 = -100; // The minimum score that must be achieved before a short queue result is accepted
        float thresh2 = 1.5f; // The amount by which the new score must be better than the last

        // device has not been moving.  Use the long queue.  Should be more accurate
        if (m_sinceMoveQueue.size()>5) {
            UpdateBestFitFromQueue(m_sinceMoveQueue, "m_sinceMoveQueue", thresh1, thresh2);
        }
        // device has moved, use the short queue
        else {
            UpdateBestFitFromQueue(m_shortQueue, "m_shortQueue", thresh1, thresh2);
        }
    }

    /**
     * Only if score is good enough in absolute sense and offers a big enough improvement over the previous location
     * @param queue which queue to use in makeing the summary.  shortQueue of recent recordings or long one since last move.
     * @param description log which queue is being used
     * @param thresh1 absolute min score
     * @param thresh2 amount by which score must improve
     */
    private void UpdateBestFitFromQueue(ReadingsQueue queue, String description, float thresh1, float thresh2){
        HashMap<Integer, List<Float>> observationSummary;
        ReadingSummaryList.ReadingSummary locationSummary;

        // Find the best fit
        observationSummary = queue.GetSummary();
        summaryList.UpdateScores(observationSummary);


        int maxIndex = -1;
        float maxScore = -1e9f;
        for (int i = 0; i<summaryList.summaryList.size(); i++) {
            locationSummary = summaryList.summaryList.get(i);
            if (locationSummary.score>maxScore){
                maxIndex = i;
                maxScore = locationSummary.score;
            }
        }
        // Decide if the best fit is good enough to use

        // No location is recorded yet
        boolean updatePos = false;
        if (bestFitIndex < 0) {
            updatePos = true;
        }
        else if (bestFitIndex==maxIndex){
            updatePos = false;
            //Log.d(TAG, "Same place");
        }
        else {
            // Find the distance to the position with the best score
            float x = summaryList.summaryList.get(maxIndex).x;
            float y = summaryList.summaryList.get(maxIndex).y;
            int level = summaryList.summaryList.get(maxIndex).level;
            double dist_px = 1000.0;
            if (level == bestFitLevel) {
                dist_px = Math.sqrt((x - bestFitX) * (x - bestFitX) + (y - bestFitY) * (x - bestFitY));
            } else {
                float dx0 = connectionPoints.getX(nearestConnectionIndex, bestFitLevel) - bestFitX;
                float dy0 = connectionPoints.getY(nearestConnectionIndex, bestFitLevel) - bestFitY;
                float dx1 = connectionPoints.getX(nearestConnectionIndex, level) - x;
                float dy1 = connectionPoints.getY(nearestConnectionIndex, level) - y;
                dist_px = Math.sqrt(dx0 * dx0 + dy0 * dy0) + Math.sqrt(dx1 * dx1 + dy1 * dy1);
            }
            double timeToThere = (((dist_px / pxPerM) - 20) / walking_pace) * 1000;

            if (timeToThere < (offset - bestFitTime)) {
                updatePos = true;
                //Log.d(TAG, "Updated because timeToThere=" + timeToThere + " and we have been here for " + (offset - bestFitTime));
            }
            else {
                //Log.d(TAG, "Not updated because timeToThere=" + timeToThere + " and we have been here for " + (offset - bestFitTime));
            }
        }
        // Criteria met for position to be updated.
        if (updatePos) {
            if (callingActivity.GetLevelID()!= summaryList.summaryList.get(maxIndex).level) {
                //Log.d(TAG, "Level changed to " + summaryList.summaryList.get(maxIndex).level);
                bestFitLevel = summaryList.summaryList.get(maxIndex).level;
                SetLevelOnUIThread(bestFitLevel);
            }
            bestFitTime = offset;
            bestFitX = summaryList.summaryList.get(maxIndex).x;
            bestFitY = summaryList.summaryList.get(maxIndex).y;
            bestFitIndex = maxIndex;
            nearestConnectionIndex = connectionPoints.IndexOfClosest(bestFitLevel, bestFitX, bestFitY);
        }
    }

    /**
     * Set the image to display.  Called when best fit level is different from currently displayed.
     * Only use this if there has been a change.
     * @param bestFitLevel - value of floor level.
     */
    private void SetLevelOnUIThread(final int bestFitLevel) {
        callingActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callingActivity.SetLevel(bestFitLevel);
            }
        });
    }

    private void UpdateOnUIThread(final List<String> scores, final float bestGuessX, final float bestGuessY)
    {
        callingActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callingActivity.UpdateLocateProgress(scores, bestGuessX, bestGuessY);
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



    private class ReadingsQueue{

        ArrayDeque<Map<Integer, Float>> values;
        int maxLength;

        public ReadingsQueue(int maxLength)
        {
            this.maxLength = maxLength;
            values = new ArrayDeque<>();

        }

        /**
         * Returns the number of elements in this deque.
         */
        public int size(){ return values.size(); }

        /**
         * Clears the contents of the Queue
         */
        public void clear() { values.clear(); }

        /**
         * Adds a new empty record to the end of the queue and removes any records from the start
         * that are too far in the past.
         * @param latestTime the time since recording started (in ms)
         */
        public void AddNew(long latestTime)
        {
            values.addLast(new HashMap<Integer, Float>());
            if (values.size()>maxLength) {
                values.removeFirst();
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
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values.clone();
            // Shake detection
            float x = mGravity[0];
            float y = mGravity[1];
            float z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = FloatMath.sqrt(x * x + y * y + z * z);

            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;
            // Make this higher or lower according to how much
            // motion you want to detect
            if(mAccel > 0.5){
                resetSinceMoveQueue = true;
                Log.d(TAG, "movement logged after: " + offset + "ms : mAccel=" + mAccel);
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}
