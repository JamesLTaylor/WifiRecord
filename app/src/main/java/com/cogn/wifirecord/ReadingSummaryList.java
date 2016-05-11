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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A list of averages readings at various locations.
 */
public class ReadingSummaryList {
    public List<ReadingSummary> summaryList;

    /**
     * Open a file and read the contents into a new ReadingSummaryList
     * @param location
     */
    public ReadingSummaryList(String location)
    {
        summaryList = new ArrayList<>();
        File fileToUse = GetMostRecentSummaryFile(location);
        if (fileToUse==null) {
            // TODO make an object that will return empty lists
            return;
        }
        BufferedReader in = null;
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
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Float> GetXList(int level) {
        List<Float> xList = new ArrayList<>();
        for (ReadingSummary summary : summaryList) {
            if (summary.level==level)
                xList.add(summary.x);
        }
        return xList;
    }

    public List<Float> GetYList(int level) {
        List<Float> yList = new ArrayList<>();
        for (ReadingSummary summary : summaryList) {
            if (summary.level==level)
                yList.add(summary.y);
        }
        return yList;
    }

    private File GetMostRecentSummaryFile(String location){
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

    /**
     * Sets the scores for the test summary comapered with each of the points in the list.
     * @param testSummary Map of macId int with a list of [p, mu, sigma] for the observations
     */
    public void UpdateScores(HashMap<Integer, List<Float>> testSummary)
    {
        for (ReadingSummary summary : summaryList) {
            summary.score = GetScore(summary.stats, testSummary);
        }
    }

    /**
     * The scores for the latest observation compared with stored locations on this level.
     * @param levelID only gets strings for currently displayed level
     * @return a list strings representing the scores of each the observation comapared to each location.
     */
    public ScoresAndBest GetScores(int levelID) {
        List<String> scores = new ArrayList<>();
        float maxScore = -1e9f;
        float score;
        float minX = 0;
        float minY = 0;
        for (ReadingSummary summary : summaryList) {
            if (summary.level==levelID) {
                score = summary.score;
                if (score>maxScore){
                    maxScore = score;
                }
                scores.add(String.format(Locale.US, "%.1f", score));
            }
        }
        ScoresAndBest result = new ScoresAndBest();
        result.scores = scores;
        return result;
    }


    private float GetScore(Map<Integer, List<Float>> recordedSummary, Map<Integer, List<Float>> obsSummary) {
        float score = 0;
        float w1 = 1;
        float w2 = 1;
        float w3 = 2;
        float totalWeighting = 0;
        for (Map.Entry<Integer, List<Float>> recordedEntry : recordedSummary.entrySet()) {
            float recordedMean = recordedEntry.getValue().get(1);
            float p = recordedEntry.getValue().get(0);
            totalWeighting += recordedEntry.getValue().get(0);
            if (obsSummary.containsKey(recordedEntry.getKey())) {
                // in fingerprint and in obs
                float d = Math.abs(recordedMean - obsSummary.get(recordedEntry.getKey()).get(1));
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
            if (!recordedSummary.containsKey(obsEntry.getKey())){
                //in obs but not fingerprint
                float obsP = obsEntry.getValue().get(0);
                float obsMean = obsEntry.getValue().get(1);
                if (obsMean > -90) {
                    score -= w3 * obsP * Math.abs(-90 - obsMean);
                }
            }
        }
        return score/totalWeighting;

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
        public float score;

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
