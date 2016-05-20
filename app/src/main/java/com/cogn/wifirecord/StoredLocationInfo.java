package com.cogn.wifirecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A list of averages readings at various locations.
 * Becomes stateful once it is being used keeping track of the current distances to a selected point
 *
 */
public class StoredLocationInfo {
    private ConnectionPoints connectionPoints;
    private List<ReadingSummary> summaryList;
    private HashSet<Integer> validMacs;
    private int nearestConnectionIndex;
    private int currentIndex;

    /**
     * Open a file and read the contents into a new StoredLocationInfo
     * @param location used to get the filename where data is stored
     */
    public StoredLocationInfo(String location, ConnectionPoints connectionPoints)
    {
        this.connectionPoints = connectionPoints;
        summaryList = new ArrayList<>();
        validMacs = new HashSet<>();
        File fileToUse = getMostRecentSummaryFile(location);
        if (fileToUse==null) {
            // TODO make an object that will return empty lists
            return;
        }
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(fileToUse));
            String str;
            ReadingSummary summary = null;
            while ((str = in.readLine()) != null) {
                String[] cols = str.split(",");
                if (cols[0].equalsIgnoreCase("LOCATION")) {
                    float x = Float.parseFloat(cols[2]);
                    float y = Float.parseFloat(cols[3]);
                    int level = (int)Float.parseFloat(cols[1]);
                    summary = new ReadingSummary(x, y, level);
                    summaryList.add(summary);
                } else if (cols.length==4)  {
                    int id = Integer.parseInt(cols[0]);
                    float p = Float.parseFloat(cols[1]);
                    float mu = Float.parseFloat(cols[2]);
                    float sigma = Float.parseFloat(cols[3]);
                    summary.add(id, p, mu, sigma);
                    validMacs.add(id);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Float> getXList(int level) {
        List<Float> xList = new ArrayList<>();
        for (ReadingSummary summary : summaryList) {
            if (summary.level==level)
                xList.add(summary.x);
        }
        return xList;
    }

    public List<Float> getYList(int level) {
        List<Float> yList = new ArrayList<>();
        for (ReadingSummary summary : summaryList) {
            if (summary.level==level)
                yList.add(summary.y);
        }
        return yList;
    }

    private File getMostRecentSummaryFile(String location){
        File locationFolder = new File(DataReadWrite.BaseFolder, location);
        if (!locationFolder.exists()) {
            locationFolder.mkdir();
        }
        File fileListing[] = locationFolder.listFiles();
        // Get the newest summary file
        Date fileDate;
        Date mostRecentDate = new Date(0);
        File fileToUse = null;
        for (File file : fileListing) {
            String fname = file.getName();
            String[] parts = fname.substring(0, fname.length()-4).split("_", 3);

            if (parts[1].equals("summary")) {
                try {
                    fileDate = DataReadWrite.timeStampFormat.parse(parts[2]);
                    if (fileDate.after(mostRecentDate)) {
                        mostRecentDate = fileDate;
                        fileToUse = file;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        return fileToUse;
    }

    /** Updates only the scores that are close enough to the current location
     * Only call after calling {@link #setCurrent(int)}
     * Sets the scores for the test summary compared with each of the points in the list.
     * @param testSummary Map of macId int with a list of [p, mu, sigma] for the observation
     * @param elapsedTimeMS time since last update in ms
     * @param marginForErrorMS distance that is allowed to travel in zero time to account for
     *                         possible errors in location
     */
    public void updateScores(HashMap<Integer, List<Float>> testSummary, double elapsedTimeMS, float marginForErrorMS)
    {
        double range = (elapsedTimeMS + marginForErrorMS)/1000;
        for (ReadingSummary summary : summaryList) {
            if (summary.timeToCurrent<=range) {
                summary.scoreToLatest = getScore(summary.stats, testSummary);
            } else
            {
                summary.scoreToLatest = -200.0f;
            }
        }
    }

    /**
     * Update all scores
     * @param testSummary Map of macId int with a list of [p, mu, sigma] for the observation
     */
    public void updateScores(HashMap<Integer, List<Float>> testSummary) {
        for (ReadingSummary summary : summaryList) {
            summary.scoreToLatest = getScore(summary.stats, testSummary);
        }
    }

    public void setCurrent(int index)
    {
        currentIndex = index;
        nearestConnectionIndex = connectionPoints.IndexOfClosest(getLevelAt(index), getXAt(index), getYAt(index));
    }

    /** Finds the distance between the point at the current index and all other points.
     */
    public void updateDistances(int index, float pxPerM, float walkingPace)
    {
        float xFrom = getXAt(index);
        float yFrom = getYAt(index);
        int levelFrom = getLevelAt(index);
        for (ReadingSummary summary : summaryList){
            float xTo = summary.x;
            float yTo = summary.y;
            int levelTo = summary.level;
            if (levelFrom == levelTo) {
                summary.distToCurrent = Math.sqrt((xFrom - xTo) * (xFrom - xTo) + (yFrom - yTo) * (yFrom - yTo));
                summary.timeToCurrent = (summary.distToCurrent/pxPerM) / walkingPace;
            } else {
                float dx0 = connectionPoints.getX(nearestConnectionIndex, levelFrom) - xFrom;
                float dy0 = connectionPoints.getY(nearestConnectionIndex, levelFrom) - yFrom;
                float dx1 = connectionPoints.getX(nearestConnectionIndex, levelTo) - xTo;
                float dy1 = connectionPoints.getY(nearestConnectionIndex, levelTo) - yTo;
                summary.distToCurrent = Math.sqrt(dx0 * dx0 + dy0 * dy0) + Math.sqrt(dx1 * dx1 + dy1 * dy1) + pxPerM*10.0;
                summary.timeToCurrent = (summary.distToCurrent/pxPerM) / walkingPace;
            }
        }

    }

    /**
     * Returns the already calcualted scores for the latest observation compared with stored
     * locations on this level.
     *
     * Scores are calculated by calling {@link #updateScores(HashMap, double, float)}
     *
     * @param levelID only gets strings for currently displayed level
     * @return a list strings representing the scores of each the observation comapared to each location.
     */
    public ScoresAndBest getScores(int levelID) {
        List<String> scores = new ArrayList<>();
        float maxScore = -1e9f;
        float score;
        for (ReadingSummary summary : summaryList) {
            if (summary.level==levelID) {
                score = summary.scoreToLatest;
                if (score>maxScore){
                    maxScore = score;
                }
                if (score>-200) {
                    scores.add(String.format(Locale.US, "%.1f", score));
                } else {
                    scores.add("");
                }
            }
        }
        ScoresAndBest result = new ScoresAndBest();
        result.scores = scores;
        return result;
    }


    /**
     * Measures how different the two observations are.
     *
     * @param recordedSummary The wifi details of a recorded observation
     * @param obsSummary The wifi details of a current observation
     * @return a value representing how close the two observations are.  Zero is the maximum
     */
    private float getScore(Map<Integer, List<Float>> recordedSummary, Map<Integer, List<Float>> obsSummary) {
        float score = 0;
        float w1 = 1;
        float w2 = 1;
        float w3 = 2;
        float totalWeighting = 0;
        for (Map.Entry<Integer, List<Float>> recordedEntry : recordedSummary.entrySet()) {
            float recordedMean = recordedEntry.getValue().get(1);
            float p = recordedEntry.getValue().get(0);
            totalWeighting += w2 * p * Math.abs(-90-recordedMean);
            if (obsSummary.containsKey(recordedEntry.getKey())) {
                // in fingerprint and in obs
                float obsMean = obsSummary.get(recordedEntry.getKey()).get(1);
                float d = Math.abs(recordedMean - obsMean);
                d = Math.max(0, d-2);
                score -= w1 * d * p;
            } else {
                // in fingerprint but not in obs
                if (recordedMean > -90) {
                    score -= w2 * p * Math.abs(-90 - recordedMean);
                }
            }
        }
        for (Map.Entry<Integer, List<Float>> obsEntry : obsSummary.entrySet()) {
            if (!recordedSummary.containsKey(obsEntry.getKey()) && validMacs.contains(obsEntry.getKey()) ){
                //in obs but not fingerprint
                float obsP = obsEntry.getValue().get(0);
                float obsMean = obsEntry.getValue().get(1);
                if (obsMean > -90) {
                    score -= w3 * obsP * Math.abs(-90 - obsMean);
                }
            }
        }
        return 100.0f*score/totalWeighting;

    }

    public int getBestScoreIndex() {
        float maxScore = -1e9f;
        int maxIndex = -1;
        for (int i = 0; i<summaryList.size(); i++) {
            ReadingSummary locationSummary = summaryList.get(i);
            if (locationSummary.scoreToLatest >maxScore){
                maxIndex = i;
                maxScore = locationSummary.scoreToLatest;
            }
        }
        return maxIndex;
    }

    public float getScoreAt(int index) {
        return summaryList.get(index).scoreToLatest;
    }

    public float getXAt(int index) {
        return summaryList.get(index).x;
    }

    public float getYAt(int index) {
        return summaryList.get(index).y;
    }

    public int getLevelAt(int index) {
        return summaryList.get(index).level;
    }

    public double getTimeToCurrent(int index) {
        return summaryList.get(index).timeToCurrent;
    }


    /**
     * Container to store the all the scores as well as the location of the best fit
     */
    public class ScoresAndBest {
        public float x;
        public float y;
        public List<String> scores;

        public ScoresAndBest(){}
    }


    /**
     * The average of readings at a single point.
     */
    public class ReadingSummary{
        public float x;
        public float y;
        public int level;
        public Map<Integer, List<Float>> stats;
        public float scoreToLatest;
        public double distToCurrent;
        public double timeToCurrent;

        public ReadingSummary(float x, float y, int level)
        {
            this.x = x;
            this.y = y;
            this.level = level;
            stats = new HashMap<>();
        }

        public void add(int id, float p, float mu, float sigma) {
            stats.put(id, Arrays.asList(p, mu, sigma));
        }
    }
}
