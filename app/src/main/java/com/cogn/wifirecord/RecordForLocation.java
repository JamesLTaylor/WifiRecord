package com.cogn.wifirecord;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.util.SparseArray;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Class to estimate location of user based on Wifi readings.
 */
public class RecordForLocation extends RecordForLocationPersistent implements SensorEventListener {
    private RecordActivity callingActivity;
    private ProvidesWifiScan wifiScanner;

    private SparseArray<Float> oldResults;
    private SparseArray<Float> results;
    private List<String> scores;
    private long startTimeMillis;

    public RecordForLocation(){
    }

    public RecordForLocation(Parameters params, RecordActivity callingActivity, ProvidesWifiScan wifiScanner) {
        this.params = params;
        pxPerDelay = params.walkingPace*params.pxPerM * delayMS/1000;
        this.callingActivity = callingActivity;
        this.wifiScanner = wifiScanner;
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
        resetSinceMoveQueue = false;
        startTimeMillis = Calendar.getInstance().getTimeInMillis();
        m_shortQueue = new ReadingsQueue(params.lengthMovingObs);
        m_sinceMoveQueue = new ReadingsQueue(params.maxLengthStationaryObs);
        scores = null;
        bestFitIndex = -1;
        oldResults = null;
    }

    public void resetReferences(RecordActivity callingActivity, ProvidesWifiScan wifiScanner) {
        this.callingActivity = callingActivity;
        this.wifiScanner = wifiScanner;
    }

    public void stop() {
        requestStop = true;
        Log.d(TAG, "SCAN STOP REQUESTED. WAITING...");
        for (int i = 0; i < 30; i++) {
            if (!scanRunning) {
                Log.d(TAG, "CONFIRMED SCAN STOP");
                requestStop = false;
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d("TAG", "Scan did not stop after 3s, something is wrong.  request stop flag left on.");
    }

    /**
     * Clears past readings
     */
    public void reset() {
        stop();
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
        resetSinceMoveQueue = false;
        m_shortQueue = new ReadingsQueue(params.lengthMovingObs);
        m_sinceMoveQueue = new ReadingsQueue(params.maxLengthStationaryObs);
        scores = null;
        bestFitIndex = -1;
        oldResults = null;
        start();
    }

    public void clearReferences() {
        this.callingActivity = null;
        this.wifiScanner = null;
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



    private void startScanning(){
        if (scanRunning) {
            Log.d(TAG, "There is already a scan running, scan not started");
            return;
        }
        scanRunning = true;
        Log.d(TAG, "Scan started");

        while (!requestStop){
            offset = Calendar.getInstance().getTimeInMillis() - startTimeMillis;
            results = wifiScanner.getScanResults(Calendar.getInstance().getTimeInMillis());
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
                scores = GlobalDataFragment.wifiFingerprintInfo.getScores(callingActivity.getLevelID()).scores;
            } else {
                updateMarkedLocation(false); // No new reading, just drift the circle if required.
            }


            if (scores!=null && !(bestFitIndex<0))
            {
                float radius = (((offset - bestFitTime)/1000.0f) * params.walkingPace + params.errorAccomodationM) * params.pxPerM;
                if (GlobalDataFragment.continuousLocate) {
                    setPositionOnUIThread(scores, currentX, currentY, bestFitX, bestFitY, radius, false);
                }
            }

            try { Thread.sleep(delayMS); }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        scanRunning = false;
        Log.d(TAG, "SCAN STOPPED");
    }

    private void updateBestFit() {
        //  nothing set yet.
        if (bestFitIndex<0) {
            setMovementStatusOnUIThread("Initial scan " + Integer.toString(m_sinceMoveQueue.size()) + "/3" );
            if (m_sinceMoveQueue.size()>=3) {
                GlobalDataFragment.wifiFingerprintInfo.updateScores(m_sinceMoveQueue.getSummary());
                int maxIndex = GlobalDataFragment.wifiFingerprintInfo.getBestScoreIndex();
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
        WifiFingerprintInfo.ReadingSummary locationSummary;

        // Find the unconstrained best fit
        observationSummary = queue.getSummary();
        double elapsedTime = (offset - bestFitTime);  // Time since the last time that the location was updated
        GlobalDataFragment.wifiFingerprintInfo.updateScores(m_shortQueue.getSummary(), elapsedTime, 1000*params.errorAccomodationM/params.walkingPace);
        int maxIndex = GlobalDataFragment.wifiFingerprintInfo.getBestScoreIndex();
        float maxScore = GlobalDataFragment.wifiFingerprintInfo.getScoreAt(maxIndex);

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
            double timeToThere = GlobalDataFragment.wifiFingerprintInfo.getTimeToCurrent(maxIndex) - params.errorAccomodationM / params.walkingPace;

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
        bestFitX = GlobalDataFragment.wifiFingerprintInfo.getXAt(maxIndex);
        bestFitY = GlobalDataFragment.wifiFingerprintInfo.getYAt(maxIndex);
        bestFitIndex = maxIndex;
        bestFitScore = GlobalDataFragment.wifiFingerprintInfo.getScoreAt(bestFitIndex);
        GlobalDataFragment.wifiFingerprintInfo.setCurrent(bestFitIndex);
        GlobalDataFragment.wifiFingerprintInfo.updateDistances(bestFitIndex, params.pxPerM, params.walkingPace);
        bestFitLevel = GlobalDataFragment.wifiFingerprintInfo.getLevelAt(maxIndex);

        if (callingActivity.getLevelID()!= GlobalDataFragment.wifiFingerprintInfo.getLevelAt(maxIndex)) {
            if (GlobalDataFragment.continuousLocate) {
                setLevelOnUIThread(bestFitLevel);
            }
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
                                       final float bestGuessX, final float bestGuessY, final float bestGuessRadius,
                                       final boolean centerViewOnCurrent)
    {
        if (callingActivity!=null) {
            callingActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    callingActivity.updateLocateProgress(scores, currentX, currentY, bestGuessX, bestGuessY, bestGuessRadius, centerViewOnCurrent);
                }
            });
        }
    }

    public void start() {
        Log.d(TAG, "SCAN START REQUESTED");
        new Thread(new Runnable() {
            public void run() {
                startScanning();
            }
        }).start();
    }

    public void sendLocation() {
        setLevelOnUIThread(bestFitLevel);
        float radius = (((offset - bestFitTime)/1000.0f) * params.walkingPace + params.errorAccomodationM) * params.pxPerM;
        List<String> scores = GlobalDataFragment.wifiFingerprintInfo.getScores(callingActivity.getLevelID()).scores;
        setPositionOnUIThread(scores, currentX, currentY, bestFitX, bestFitY, radius, true);

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

}
