package com.cogn.wifirecord;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MacLookup {
    private static final String TAG = "WIFI";
    private List<String> macs;
    private List<Integer> ids;
    private List<String> ssids;
    private File file;
    private boolean updateFile;

    /**
     * Mac lookup for locating.  No new macs are added
     */
    public MacLookup(InputStream macInputStream)
    {
        updateFile = false;
        macs = new ArrayList<>();
        ids = new ArrayList<>();
        ssids = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(macInputStream));
        try {
            String str;
            while ((str = reader.readLine()) != null) {
                String[] cols = str.split(",");
                macs.add(cols[0]);
                ssids.add(cols[1]);
                ids.add(Integer.parseInt(cols[2]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Mac lookup for recording, new macs are added as they are seen.
     */
    public MacLookup(String location, String filename)
    {
        updateFile = true;
        String folderName = "WifiRecord/"+location;
        macs = new ArrayList<>();
        ids = new ArrayList<>();
        ssids = new ArrayList<>();
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
        BufferedReader in;
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

    public Integer getID(String mac)
    {
        return macs.indexOf(mac);
    }

    public String getMac(Integer id){
        return macs.get(id);
    }


    /**
     * Get ID and possibly write to a file.  Writing to a file will be determined by whihc
     * constructor is used
     * @param mac media access control address of the router as hex pairs: 01-23-45-67-89-ab
     * @param ssid the access point (AP) name.
     * @return the shorthand version/index of the provided mac
     */
    public Integer getId(String mac, String ssid){
        int pos = macs.indexOf(mac);
        if (pos>=0) {
            return pos;
        } else {
            macs.add(mac);
            ssids.add(ssid);
            if (updateFile) {
                BufferedWriter filewriter;
                try {
                    filewriter = new BufferedWriter(new FileWriter(file, true));
                    filewriter.write(mac + "," + ssid + "," + (macs.size() - 1) + "\n");
                    filewriter.close();
                } catch (IOException e) {
                    Log.e(TAG, "could not make file", e);
                    return -1;
                }
                return macs.size()-1;
            }
            else  {
                return 10000+macs.size()-1;
            }
        }
    }
}
