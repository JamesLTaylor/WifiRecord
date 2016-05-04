package com.cogn.wifirecord;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Will obtain the coordinates of previous loggings and the details of the summaries for a given
 * location.
 */
public class PreviousRecordings {
    public ListPair GetPreviousRecordings(String location)
    {
        ListPair listPair = new ListPair();
        File wifiFolder = new File(Environment.getExternalStorageDirectory(), "WifiRecord");
        if (!wifiFolder.exists()) {
            wifiFolder.mkdir();
            return listPair;
        }
        File locationFolder = new File(Environment.getExternalStorageDirectory(),"WifiRecord/" + location);
        if (!locationFolder.exists()) {
            locationFolder.mkdir();
            return listPair;
        }
        File fileListing[] = locationFolder.listFiles();
        // Check if there is a summary file

        return listPair;

    }

    public class ListPair{
        public ArrayList<Float> xList;
        public ArrayList<Float> yList;
        public ListPair(){
            xList = new ArrayList<Float>();
            yList = new ArrayList<Float>();
        }
    }

}
