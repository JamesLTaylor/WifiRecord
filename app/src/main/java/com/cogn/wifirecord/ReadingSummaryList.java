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
import java.util.Map;

/**
 * Created by James on 5/6/2016.
 */
public class ReadingSummaryList {
    private List<ReadingSummary> summaryList;

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
                    int level = Integer.parseInt(cols[1]);
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


    private class ReadingSummary{
        public float x;
        public float y;
        public int level;
        public Map<Integer, List<Float>> stats;

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
