package com.cogn.wifirecord;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ContinuousRecordActivity extends Activity
        implements DialogInterface.OnDismissListener, DialogInterface.OnKeyListener,
        DialogInterface.OnCancelListener, View.OnClickListener
{

    private static final String TAG = "CONTINUOUS RECORD";
    private long startTimeMillis;
    private int counter;
    private static boolean scanRunning;
    private WifiManager wifiManager;
    private MacLookup macLookup;
    private String location;
    private String deviceName;

    private static String filename;
    private static TextView textView;
    private static String macName;
    private String description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_continuous_record);
        textView = (TextView)findViewById(R.id.continuous_record_info);
        Intent myIntent = getIntent();
        location = myIntent.getStringExtra("location");
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        deviceName = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_general_device_name), "");
        if (deviceName.length() < 4) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton("OK", null);
            builder.setTitle("Error");
            builder.setMessage("Device name not set.  Please set a name of at least 3 characters under Settings->General");
            builder.setOnDismissListener(this);
            builder.setOnKeyListener(this);
            builder.setOnCancelListener(this);
            builder.show();
            //finish();
            return;
        }

        //TODO: Store these strings in a text file on the device
        String[] DESCRIPTIONS = getPathDescriptionsFromFile();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, DESCRIPTIONS);
        AutoCompleteTextView textView = (AutoCompleteTextView)
                findViewById(R.id.continuous_record_description);
        textView.setAdapter(adapter);

        findViewById(R.id.continuous_record_start).setOnClickListener(this);

        if (scanRunning) {
            LinearLayout layout = (LinearLayout) findViewById(R.id.continuous_record_layout);
            View view = findViewById(R.id.continuous_record_description);
            layout.removeView(view);
            findViewById(R.id.continuous_record_start).setEnabled(false);
            description = savedInstanceState.getString("description");
            textView.setText(description);

        } else if (savedInstanceState!=null) {
            description = savedInstanceState.getString("description");
            textView.setText(description);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            stop();
        } else {
            //It's an orientation change.
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("description", description);
        outState.putString("filename", filename);
        outState.putString("macName", macName);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }



    public void updateResults(String value)
    {
        textView.setText(value);
    }

    private void writeToUIThread(final String newText)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateResults(newText);
            }
        });
    }

    public void stop() {
        scanRunning = false;
    }

    public void start() {
        scanRunning = true;
        Log.d(TAG, "SCAN STARTED");
        new Thread(new Runnable() {
            public void run() {
                startScanning();
            }
        }).start();
    }

    private boolean haveChanged(ArrayList<Integer> oldScanned, List<ScanResult> scanned) {
        if (oldScanned==null) return true;
        if (oldScanned.size()!=scanned.size()) return true;
        for (int i = 0; i<oldScanned.size(); i++)
        {
            if (oldScanned.get(i)!=scanned.get(i).level) return true;
        }
        return false;
    }

    private void startScanning() {
        Calendar c = Calendar.getInstance();
        // Make the file and folders
        File folder = new File(Environment.getExternalStorageDirectory(), "WifiRecord/"+location);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, filename);
        try {
            file.createNewFile();
            BufferedWriter filewriter = new BufferedWriter(new FileWriter(file, true));
            filewriter.write("DEVICE," + android.os.Build.BRAND + "," + android.os.Build.MODEL + "\n");
            filewriter.write("DESCRIPTION," + description + "\n");
            filewriter.close();

        } catch (IOException ioe)
        {
            Log.e(TAG, "could not make file", ioe);
            return;
        }

        // Start recording

        ArrayList<Integer> oldScanned = null;
        List<ScanResult> scanned;
        long offset;
        int macID;
        scanRunning = true;
        startTimeMillis = c.getTimeInMillis();


        while (scanRunning){
            scanned = wifiManager.getScanResults();
            if (haveChanged(oldScanned, scanned)) {
                try {
                    String results = "";
                    BufferedWriter filewriter = new BufferedWriter(new FileWriter(file, true));
                    offset = Calendar.getInstance().getTimeInMillis() - startTimeMillis;
                    Log.d(TAG, "OFFSET," + offset + "\n");
                    filewriter.write("OFFSET," + offset + "\n");
                    results+="OFFSET," + offset + "\n";
                    oldScanned = new ArrayList<Integer>();
                    for (ScanResult scan : scanned) {
                        macID = macLookup.GetId(scan.BSSID, scan.SSID);
                        Log.d(TAG, macID + "," + scan.level + "\n");
                        filewriter.write(macID + "," + scan.level + "\n");
                        results+=""  + macID + "," + scan.level + "\n";
                        oldScanned.add(scan.level);
                    }
                    writeToUIThread(results);
                    filewriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                long delayMS = 100;
                Thread.sleep(delayMS); }
            catch (InterruptedException e) {
                e.printStackTrace();
                scanRunning = false;
            }
            wifiManager.startScan();
        }
        // Scan finished.  Take a copy of the macs.

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        dialog.dismiss();
        return false;
    }

    @Override
    public void onClick(View v) {
        description = ((TextView)findViewById(R.id.continuous_record_description)).getText().toString().toUpperCase();
        updatePathDescriptionFile(description);
        findViewById(R.id.continuous_record_start).setEnabled(false);
        LinearLayout layout = (LinearLayout) findViewById(R.id.continuous_record_layout);
        View view = findViewById(R.id.continuous_record_description);
        layout.removeView(view);

        String scanStartTime = DataReadWrite.timeStampFormat.format(Calendar.getInstance().getTime());
        filename = location.toLowerCase().trim() + "_" + scanStartTime + "_" + deviceName + "_path.txt";
        macName = location.toLowerCase().trim() + "_" + scanStartTime + "_" + deviceName + "_macs.txt";
        macLookup = new MacLookup(location, macName);
        start();
    }

    private File getPathDescriptionFile()
    {
        String folderName = "WifiRecord/"+location;
        File folder = new File(Environment.getExternalStorageDirectory(), folderName);
        String descriptionFilename = location + "_path_descriptions.txt";
        File file = new File(folder, descriptionFilename);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ioe)
            {
                Log.e(TAG, "could not make file", ioe);
                return null;
            }
        }
        return file;
    }

    private void updatePathDescriptionFile(String description) {
        boolean found = false;
        for (String string : getPathDescriptionsFromFile()){
            if (string.compareToIgnoreCase(description)==0)
                return;
        }
        File file = getPathDescriptionFile();
        try {
            BufferedWriter filewriter = new BufferedWriter(new FileWriter(file, true));
            filewriter.write(description.toUpperCase()+"\n");
            filewriter.close();
        } catch (IOException e) {
            Log.e(TAG, "could not make file", e);
        }
    }

    private String[] getPathDescriptionsFromFile() {
        List<String> descriptionList = new ArrayList<>();

        BufferedReader in = null;
        File file = getPathDescriptionFile();
        try {
            in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                descriptionList.add(str);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] returnArray = new String[descriptionList.size()];
        return descriptionList.toArray(returnArray);
    }
}
