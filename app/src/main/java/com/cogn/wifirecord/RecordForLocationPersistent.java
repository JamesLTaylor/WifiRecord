package com.cogn.wifirecord;

import android.util.SparseArray;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordForLocationPersistent {
    protected static final String TAG = "WIFI_LOCATE";

    protected static boolean scanRunning = false;
    protected boolean requestStop = false;
    protected long delayMS = 100;

    protected RecordForLocation.Parameters params;

    protected double mAccel;
    protected double mAccelCurrent;
    protected double mAccelLast;
    protected boolean resetSinceMoveQueue;

    //For location
    protected long offset;
    protected float currentX; // where the circle is currently drawn
    protected float currentY;
    protected float bestFitX; // the current best guess place
    protected float bestFitY;
    protected int bestFitLevel;
    protected int bestFitIndex;
    protected long bestFitTime;
    protected float bestFitScore;
    protected ReadingsQueue m_shortQueue;
    protected ReadingsQueue m_sinceMoveQueue;

    // Values for drifting circle
    protected long prevTime = 0;
    protected float dx = 0;
    protected float dy = 0;
    protected float pxPerDelay;

    public RecordForLocationPersistent(RecordForLocationPersistent original) {
        this.bestFitIndex = original.bestFitIndex;
        this.bestFitLevel = original.bestFitLevel;
        this.bestFitScore = original.bestFitScore;
        this.bestFitTime = original.bestFitTime;
        this.bestFitX = original.bestFitX;
        this.bestFitY = original.bestFitY;
        this.currentX = original.currentX;
        this.currentY = original.currentY;
        this.delayMS = original.delayMS;
        this.dx = original.dx;
        this.dy = original.dy;
        this.m_shortQueue = original.m_shortQueue;
        this.m_sinceMoveQueue = original.m_sinceMoveQueue;
        this.mAccel = original.mAccel;
        this.mAccelCurrent = original.mAccelCurrent;
        this.mAccelLast = original.mAccelLast;
        this.offset = original.offset;
        this.params = original.params;
        this.prevTime = original.prevTime;
        this.pxPerDelay = original.pxPerDelay;
        this.requestStop = original.requestStop;
        this.resetSinceMoveQueue = original.resetSinceMoveQueue;
    }

    public RecordForLocationPersistent() {
    }

    protected class ReadingsQueue{

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
