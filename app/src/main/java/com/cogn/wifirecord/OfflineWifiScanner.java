package com.cogn.wifirecord;

import android.os.Environment;
import android.util.SparseArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OfflineWifiScanner implements ProvidesWifiScan {

    private List<SparseArray<Float>> wifiReadings;
    private Long[] times;
    private MacLookup summaryMacs = null;
    private MacLookup pathMacs = null;


    public OfflineWifiScanner(String filename, String location, MacLookup summaryMacs, MacLookup pathMacs)
    {
        this.summaryMacs = summaryMacs;
        this.pathMacs = pathMacs;
        String folderName = "WifiRecord/"+location;
        File folder = new File(Environment.getExternalStorageDirectory(), folderName);
        File file = new File(folder, filename);
        try {
            Reader reader = new FileReader(file);
            loadFile(reader);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public OfflineWifiScanner(InputStream inputStream){
        loadFile(new InputStreamReader(inputStream));
    }

    public SparseArray<Float> getScanResults(long atTime){
        int index = Arrays.binarySearch(times, atTime);
        if (index>0) {
            if (index>=wifiReadings.size()) {
                return wifiReadings.get(wifiReadings.size()-1);
            } else {
                return wifiReadings.get(index);
            }
        } else {
            index = -index;
            if (index>=wifiReadings.size()) {
                return wifiReadings.get(wifiReadings.size()-1);
            } else {
                return wifiReadings.get(index);
            }
        }
    }

    public long getTotalRecordingTime(){
        return times[times.length-1];
    }

    private void loadFile(Reader reader){
        BufferedReader in = new BufferedReader(reader);
        wifiReadings = new ArrayList<>();
        ArrayList<Long> timesArrayList = new ArrayList<>();

        try {
            //in = new BufferedReader(new FileReader(file));
            String str;
            SparseArray<Float> reading = null;
            while ((str = in.readLine()) != null) {
                String[] cols = str.split(",");
                if (cols[0].equalsIgnoreCase("OFFSET")){
                    timesArrayList.add(Long.parseLong(cols[1]));
                    reading = new SparseArray<>();
                    wifiReadings.add(reading);
                } else if (cols.length==2) {
                    Integer macID;
                    Float level;
                    try {
                        macID = Integer.parseInt(cols[0]);
                        level = Float.parseFloat(cols[1]);
                        if (summaryMacs!=null) {
                            macID = summaryMacs.getID(pathMacs.getMac(macID));
                        }
                        reading.put(macID, level);
                    }
                    catch (NumberFormatException nfe) {}
                    catch (NullPointerException npe) {}
                }
            }
            in.close();
            times = new Long[timesArrayList.size()];
            times = timesArrayList.toArray(times);
        //} catch (FileNotFoundException e) {
        //    e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
