package com.cogn.wifirecord;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MacLookup {
    private static final String TAG = "WIFI";
    private List<String> macs;
    private List<Integer> ids;
    private List<String> ssids;
    private File file;

    public MacLookup(String location)
    {
        String folderName = "WifiRecord/"+location;
        String fileName = location.toLowerCase().trim() + "_macs.txt";
        init(folderName, fileName);
    }

    public MacLookup(String location, String fileName)
    {
        String folderName = "WifiRecord/"+location;
        init(folderName, fileName);
    }

    private void init(String folderName, String filename)
    {
        macs = new ArrayList<String>();
        ids = new ArrayList<Integer>();
        ssids = new ArrayList<String>();
        File folder = new File(Environment.getExternalStorageDirectory(), folderName);
        file = new File(folder, filename);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ioe)
            {
                Log.e(TAG, "could not make file", ioe);
                return;
            }
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                String[] cols = str.split(",");
                macs.add(cols[0]);
                ssids.add(cols[1]);
                ids.add(Integer.parseInt(cols[2]));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Integer GetId(String mac, String ssid){
        int pos = macs.indexOf(mac);
        if (pos>=0)
        {
            return pos;
        }
        else
        {
            macs.add(mac);
            ssids.add(ssid);
            BufferedWriter filewriter;
            try {
                filewriter = new BufferedWriter(new FileWriter(file, true));
                filewriter.write(mac + "," + ssid +"," + (macs.size()-1)+"\n");
                filewriter.close();
            } catch (IOException e) {
                Log.e(TAG, "could not make file", e);
                return -1;
            }

            return macs.size()-1;
        }
    }

}
