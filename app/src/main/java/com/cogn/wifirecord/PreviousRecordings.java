package com.cogn.wifirecord;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Will obtain the coordinates of previous loggings and the details of the summaries for a given
 * location.
 */
public class PreviousRecordings {
    Map<Integer, ListPair> levelsAndPoints;
    public PreviousRecordings(String location)
    {
        levelsAndPoints = GetPreviousRecordings(location);
    }

    private Map<Integer, ListPair> GetPreviousRecordings(String location)
    {
        Map<Integer, ListPair> levelsAndPoints = new HashMap<Integer, ListPair>();
        //PreviousRecordings.ListPair listPair = new PreviousRecordings.ListPair();
        File wifiFolder = new File(DataReadWrite.BaseFolder);
        if (!wifiFolder.exists()) {
            wifiFolder.mkdir();
            return levelsAndPoints;
        }
        File locationFolder = new File(DataReadWrite.BaseFolder, location);
        if (!locationFolder.exists()) {
            locationFolder.mkdir();
            return levelsAndPoints;
        }
        File fileListing[] = locationFolder.listFiles();
        // Check if there is a summary file
        Date summaryDate = new Date(0);
        boolean summaryFound = false;
        for (File file : fileListing) {
            String fname = file.getName();
            String[] parts = fname.substring(0, fname.length()-4).split("_", 3);
            if (parts[1].equals("summary")) {
                try {
                    summaryDate = DataReadWrite.timeStampFormat.parse(parts[2]);
                    summaryFound = true;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        for (File file : fileListing) {
            String fname = file.getName();
            String[] parts = fname.substring(0, fname.length()-4).split("_", 3);
            if (parts[1].equals("readings")){
                try {
                    Date date = DataReadWrite.timeStampFormat.parse(parts[2]);
                    if (!summaryFound || date.after(summaryDate)) {
                        ParseFile(file, levelsAndPoints);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        return levelsAndPoints;
    }

    private void ParseFile(File file, Map<Integer, ListPair> levelsAndPoints)
    {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                String[] cols = str.split(",");
                if (cols[0].equalsIgnoreCase("NEW")){
                    float x = Float.parseFloat(cols[1]);
                    float y = Float.parseFloat(cols[2]);
                    int level = Integer.parseInt(cols[3]);
                    if (!levelsAndPoints.containsKey(level)) {
                        levelsAndPoints.put(level, new ListPair());
                    }
                    levelsAndPoints.get(level).xList.add(x);
                    levelsAndPoints.get(level).yList.add(y);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Float> GetXList(int level) {
        if (levelsAndPoints.containsKey(level)){
            return levelsAndPoints.get(level).xList;
        } else {
            return null;
        }
    }

    public List<Float> GetYList(int level) {
        if (levelsAndPoints.containsKey(level)){
            return levelsAndPoints.get(level).yList;
        } else {
            return null;
        }
    }

    private class ListPair{
        public ArrayList<Float> xList;
        public ArrayList<Float> yList;
        public ListPair(){
            xList = new ArrayList<Float>();
            yList = new ArrayList<Float>();
        }
    }

}
