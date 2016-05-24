package com.cogn.wifirecord;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.util.SparseArray;

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
    private final StoredLocationInfo storedLocationInfo;
    private RecordActivity callingActivity;
    private ProvidesWifiScan wifiScanner;
    private boolean scanRunning = false;
    private long delayMS = 100;

    private Parameters params;

    private double mAccel;
    private double mAccelCurrent;
    private double mAccelLast;
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
    private float bestFitScore;
    private ReadingsQueue m_shortQueue;
    private ReadingsQueue m_sinceMoveQueue;

    // Values for drifting circle
    private long prevTime = 0;
    private float dx = 0;
    private float dy = 0;
    private float pxPerDelay;

    public RecordForLocation(){
        storedLocationInfo = null;
    }

    public RecordForLocation(Parameters params,
                             StoredLocationInfo storedLocationInfo,
                             RecordActivity callingActivity, ProvidesWifiScan wifiScanner) {
        this.params = params;
        pxPerDelay = params.walkingPace*params.pxPerM * delayMS/1000;
        this.storedLocationInfo = storedLocationInfo;
        this.callingActivity = callingActivity;
        this.wifiScanner = wifiScanner;
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
    }

    public void Stop() {
        scanRunning = false;
        //Log.d(TAG, "SCAN STOPPED");
    }


    private boolean haveChanged(SparseArray<Float> oldResults, SparseArray<Float> results) {
        if (oldResults==null) return true;
        if (oldResults.size()!=results.size()) return true;
        for (int i = 0; i<oldResults.size(); i++)
        {
            if ((Math.abs(oldResults.valueAt(i)-results.valueAt(i))>1e-9)) return true;
        }
        return false;
    }

    private void updateMarkedLocation(boolean checkDirection)
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



    public void startScanning(){
        resetSinceMoveQueue = false;
        SparseArray<Float> oldResults = null;
        SparseArray<Float> results;
        long startTimeMillis = Calendar.getInstance().getTimeInMillis();
        m_shortQueue = new ReadingsQueue(params.lengthMovingObs);
        m_sinceMoveQueue = new ReadingsQueue(params.maxLengthStationaryObs);
        List<String> scores = null;
        bestFitIndex = -1;

        while (scanRunning){
            offset = Calendar.getInstance().getTimeInMillis() - startTimeMillis;
            results = wifiScanner.getScanResults(offset);
            if (haveChanged(oldResults, results)) {
                // Add the scan to the Queues
                m_shortQueue.addNew(offset);
                if (resetSinceMoveQueue && bestFitIndex>=0) {
                    m_sinceMoveQueue.clear();
                    resetSinceMoveQueue = false;
                }
                m_sinceMoveQueue.addNew(offset);
                oldResults = results.clone();
                for (int i = 0; i<results.size(); i++) {
                    m_shortQueue.updateEnd(results.keyAt(i), results.valueAt(i));
                    m_sinceMoveQueue.updateEnd(results.keyAt(i), results.valueAt(i));
                }

                // Find the best fit location, sets bestFitX and bestFitY
                updateBestFit();
                // Check which direction the bestGuess should move
                updateMarkedLocation(true);
                // Get the scores to display on the floorMap
                scores = storedLocationInfo.getScores(callingActivity.getLevelID()).scores;
            } else {
                updateMarkedLocation(false); // No new reading, just drift the circle if required.
            }


            if (scores!=null && !(bestFitIndex<0))
            {
                float radius = (((offset - bestFitTime)/1000.0f) * params.walkingPace + params.errorAccomodationM) * params.pxPerM;
                setPositionOnUIThread(scores, currentX, currentY, bestFitX, bestFitY, radius);
            }

            try { Thread.sleep(delayMS); }
            catch (InterruptedException e) {
                e.printStackTrace();
                scanRunning = false;
            }
        }
    }

    private void updateBestFit() {
        //  nothing set yet.
        if (bestFitIndex<0) {
            setMovementStatusOnUIThread("Initial scan " + Integer.toString(m_sinceMoveQueue.size()) + "/3" );
            if (m_sinceMoveQueue.size()>=3) {
                storedLocationInfo.updateScores(m_sinceMoveQueue.getSummary());
                int maxIndex = storedLocationInfo.getBestScoreIndex();
                updateBestFit(maxIndex);
                currentX = bestFitX; // Circle starts at best fit
                currentY = bestFitY;
            }
        }
        // device has not been moving.  Use the long queue.  Should be more accurate
        else if (m_sinceMoveQueue.size()>params.minLengthStationaryObs) {
            setMovementStatusOnUIThread("Stationary");
            updateBestFitFromQueue(m_sinceMoveQueue, "m_sinceMoveQueue");
        }
        // device has moved, use the short queue
        else {
            setMovementStatusOnUIThread("Moving");
            updateBestFitFromQueue(m_shortQueue, "m_shortQueue");
        }
    }

    /**
     * Checks if there are any points that offer better scores than the current.
     * Only if score is good enough in absolute sense and offers a big enough improvement over the previous location
     * @param queue which queue to use in makeing the summary.  shortQueue of recent recordings or long one since last move.
     * @param description log which queue is being used
     */
    private void updateBestFitFromQueue(ReadingsQueue queue, String description){
        HashMap<Integer, List<Float>> observationSummary;
        StoredLocationInfo.ReadingSummary locationSummary;

        // Find the unconstrained best fit
        observationSummary = queue.getSummary();
        double elapsedTime = (offset - bestFitTime);  // Time since the last time that the location was updated
        storedLocationInfo.updateScores(m_shortQueue.getSummary(), elapsedTime, 1000*params.errorAccomodationM/params.walkingPace);
        int maxIndex = storedLocationInfo.getBestScoreIndex();
        float maxScore = storedLocationInfo.getScoreAt(maxIndex);

        // Decide if the best fit is good enough to use
        boolean updatePos = false;
        // At the same place, only consider updating is the score has improved,
        // otherwise we could be on our way to somewhere else and we anchor this point too strongly
        if (bestFitIndex==maxIndex) {
            if (maxScore > bestFitScore){
                if (params.updateForSamePos) {
                    updatePos = true;
                    //Log.d(TAG, "Same place - update because score improved and settings allow");
                } else {
                    updatePos = false;
                    //Log.d(TAG, "Same place - not update because improved but settings do not allow");
                }
            } else {
                updatePos = false;
                //Log.d(TAG, "Same place - not update because not improved score");
            }
        }
        // Have not been at current location long and new location does not offer a significant
        // improvement.  So don't update.
        else if (maxScore<(bestFitScore + params.stickyMinImprovement) &&
                (offset-bestFitTime)<=params.stickyMaxTime){
            updatePos = false;
            //Log.d(TAG, "Sticky time no update");
        }
        // Default case, there is a better score at a new location. Check whether it is reasonable
        // that we could have walked there in the time since the current location was recorded.
        else {
            // Find the distance to the position with the best score
            double timeToThere = storedLocationInfo.getTimeToCurrent(maxIndex) - params.errorAccomodationM / params.walkingPace;

            if (timeToThere < elapsedTime) {
                updatePos = true;
                //Log.d(TAG, "Updated because timeToThere=" + timeToThere + " and we have been here for " + (offset - bestFitTime));
            }
            else {
                //Log.d(TAG, "Not updated because timeToThere=" + timeToThere + " and we have been here for " + (offset - bestFitTime));
            }
        }
        // Criteria met for position to be updated.
        if (updatePos) {
            updateBestFit(maxIndex);
        }
    }

    private void updateBestFit(int maxIndex)
    {
        bestFitTime = offset;
        //currentX = bestFitX; // Don't fall too far behind
        //currentY = bestFitY;
        bestFitX = storedLocationInfo.getXAt(maxIndex);
        bestFitY = storedLocationInfo.getYAt(maxIndex);
        bestFitIndex = maxIndex;
        bestFitScore = storedLocationInfo.getScoreAt(bestFitIndex);
        storedLocationInfo.setCurrent(bestFitIndex);
        storedLocationInfo.updateDistances(bestFitIndex, params.pxPerM, params.walkingPace);

        if (callingActivity.getLevelID()!= storedLocationInfo.getLevelAt(maxIndex)) {
            bestFitLevel = storedLocationInfo.getLevelAt(maxIndex);
            setLevelOnUIThread(bestFitLevel);
            currentX = bestFitX; // Circle does not need to drift accross levels.
            currentY = bestFitY;
        }
    }

    private void setMovementStatusOnUIThread(final String movementStatus) {
        callingActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callingActivity.updateMovementStatus(movementStatus);
            }
        });
    }

    /**
     * Set the image to display.  Called when best fit level is different from currently displayed.
     * Only use this if there has been a change.
     * @param bestFitLevel - value of floor level.
     */
    private void setLevelOnUIThread(final int bestFitLevel) {
        callingActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callingActivity.setLevel(bestFitLevel);
            }
        });
    }

    private void setPositionOnUIThread(final List<String> scores, final float currentX, final float currentY,
                                       final float bestGuessX, final float bestGuessY, final float bestGuessRadius)
    {
        callingActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callingActivity.updateLocateProgress(scores, currentX, currentY, bestGuessX, bestGuessY, bestGuessRadius);
            }
        });
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



    private class ReadingsQueue{

        ArrayDeque<SparseArray<Float>> values;
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
        public void addNew(long latestTime)
        {
            values.addLast(new SparseArray<Float>());
            if (values.size()>maxLength) {
                values.removeFirst();
            }
        }

        public void updateEnd(Integer macID, Float reading)
        {
            values.peekLast().put(macID, reading);
        }

        private float getMean(List<Float> values) {
            double total = 0.0;
            for (Float value : values)
                total += value;
            return (float)(total/values.size());
        }

        /**
         * Population standard deviation in case there is only one entry point
         */
        private float getStd(List<Float> values) {
            double total = 0.0;
            float mean = getMean(values);
            for (Float value : values)
                total += (value-mean)*(value-mean);
            return (float)Math.sqrt(total/values.size());
        }


        public HashMap<Integer, List<Float>> getSummary() {
            HashMap<Integer, ArrayList<Float>> aggregate = new HashMap<>();
            for (SparseArray<Float> value : values)
            {
                for (int i=0; i<value.size(); i++) {
                //for (Map.Entry<Integer, Float> entry : value. value.entrySet()) {
                    if (!aggregate.containsKey(value.keyAt(i))) {
                        aggregate.put(value.keyAt(i), new ArrayList<>(Arrays.asList(value.valueAt(i))));
                    }
                    else {
                        aggregate.get(value.keyAt(i)).add(value.valueAt(i));
                    }
                }
            }
            HashMap<Integer, List<Float>> summary = new HashMap<>();
            float p;
            float mu;
            float sigma;
            for (Map.Entry<Integer, ArrayList<Float>> aggregateEntry : aggregate.entrySet()) {
                p = aggregateEntry.getValue().size()/values.size();
                mu = getMean(aggregateEntry.getValue());
                sigma = getStd(aggregateEntry.getValue());
                summary.put(aggregateEntry.getKey(), new ArrayList<>(Arrays.asList(p, mu, sigma)));
            }
            return summary;
        }
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] mGravity = event.values.clone();
            // Shake detection
            float x = mGravity[0];
            float y = mGravity[1];
            float z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = Math.sqrt(x * x + y * y + z * z);

            double delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9 + delta;
            // Make this higher or lower according to how much
            // motion you want to detect
            if(mAccel > 0.5){
                resetSinceMoveQueue = true;
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public class Parameters {
        //adjustable parameters
        public float pxPerM;
        public float walkingPace =  2.0f; // m/s FAST: 7.6km/h;
        public float errorAccomodationM = 20.0f; // Distance that is allowed to move in zero time
        public int lengthMovingObs = 3;
        public int minLengthStationaryObs = 5;
        public int maxLengthStationaryObs = 20;
        public boolean updateForSamePos = false;
        public float stickyMinImprovement = 5.0f; // The amount by which the new score must be better than the last during the sticky period
        public int stickyMaxTime = 3000;

        public Parameters(float pxPerM, float walkingPace, float errorAccomodationM, int lengthMovingObs,
                          int minLengthStationaryObs, int maxLengthStationaryObs, boolean updateForSamePos,
                          float stickyMinImprovement, int stickyMaxTime)
        {
            this.pxPerM = pxPerM;
            this.walkingPace = walkingPace;
            this.errorAccomodationM = errorAccomodationM;
            this.lengthMovingObs = lengthMovingObs;
            this.minLengthStationaryObs = minLengthStationaryObs;
            this.maxLengthStationaryObs = maxLengthStationaryObs;
            this.updateForSamePos = updateForSamePos;
            this.stickyMinImprovement = stickyMinImprovement;
            this.stickyMaxTime = stickyMaxTime;
        }
    }
}
