package com.cogn.wifirecord;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LoadTestActivity extends Activity
implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    public static final String EXTRA_FILENAME = "com.cogn.wifirecord.LOAD_TEST_FILENAME";
    public static final String EXTRA_CENTER_NAME = "com.cogn.wifirecord.EXTRA_CENTER_NAME";;

    private Map<String, Map<String, Map<String, String>>> devicePathDateFname;  //Device, description, date, filename
    private String centerName;
    private Spinner deviceSpinner;
    private Spinner descriptionSpinner;
    private Spinner datetimeSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_test);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent myIntent = getIntent();
        String centerName = myIntent.getStringExtra("location");
        getFiles(centerName);

        deviceSpinner = (Spinner) findViewById(R.id.load_test_device_name);
        descriptionSpinner = (Spinner) findViewById(R.id.load_test_path_name);
        datetimeSpinner = (Spinner) findViewById(R.id.load_test_datetime);
        deviceSpinner.setOnItemSelectedListener(this);
        descriptionSpinner.setOnItemSelectedListener(this);
        datetimeSpinner.setOnItemSelectedListener(this);

        Set<String> deviceSet = devicePathDateFname.keySet();
        String[] deviceArray = new String[deviceSet.size()];
        deviceSet.toArray(deviceArray);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, deviceArray);
        Spinner spinner = (Spinner) findViewById(R.id.load_test_device_name);
        deviceSpinner.setAdapter(adapter);

        ((Button)findViewById(R.id.load_test_start)).setOnClickListener(this);

    }

    private void getFiles(String location){
        devicePathDateFname = new HashMap<>();
        File locationFolder = new File(DataReadWrite.BaseFolder, location);
        if (!locationFolder.exists()) {
            locationFolder.mkdir();
        }
        File fileListing[] = locationFolder.listFiles();

        for (File file : fileListing) {
            String fname = file.getName();
            String[] parts = fname.substring(0, fname.length() - 4).split("_");
            if (parts.length == 5 && parts[4].equalsIgnoreCase("path")) {
                BufferedReader bufReader = null;
                try {
                    bufReader = new BufferedReader(new FileReader(file));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                String description = "";
                String str;
                int count = 0;
                try {
                    while ((str = bufReader.readLine()) != null && count < 10) {
                        String[] cols = str.split(",");
                        if (cols[0].equalsIgnoreCase("DESCRIPTION")) {
                            description = cols[1];
                        }
                        count++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String device = parts[3];
                String datetime = parts[1] + "_" + parts[2];
                if (!devicePathDateFname.containsKey(device)) {
                    devicePathDateFname.put(device, new HashMap<String, Map<String, String>>());
                }
                if (!devicePathDateFname.get(device).containsKey(description)) {
                    devicePathDateFname.get(device).put(description, new HashMap<String, String>());
                }
                devicePathDateFname.get(device).get(description).put(datetime, fname);
            }
        }
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == deviceSpinner.getId()){
            descriptionSpinner.setEnabled(true);
            datetimeSpinner.setEnabled(false);

            String device = deviceSpinner.getSelectedItem().toString();

            Set<String> descriptionSet = devicePathDateFname.get(device).keySet();
            String[] descriptionArray = new String[descriptionSet.size()];
            descriptionSet.toArray(descriptionArray);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, descriptionArray);
            descriptionSpinner.setAdapter(adapter);
        }
        if (parent.getId() == descriptionSpinner.getId()){
            datetimeSpinner.setEnabled(true);

            String device = deviceSpinner.getSelectedItem().toString();
            String description = descriptionSpinner.getSelectedItem().toString();

            Set<String> set = devicePathDateFname.get(device).get(description).keySet();
            String[] array = new String[set.size()];
            set.toArray(array);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, array);
            datetimeSpinner.setAdapter(adapter);
        }
        if (parent.getId() == datetimeSpinner.getId()){
            String device = deviceSpinner.getSelectedItem().toString();
            String description = descriptionSpinner.getSelectedItem().toString();
            String datetime = datetimeSpinner.getSelectedItem().toString();

            String filename = devicePathDateFname.get(device).get(description).get(datetime);
            ((TextView)findViewById(R.id.load_test_filename)).setText(filename);
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onClick(View v) {
        String filename = ((TextView)findViewById(R.id.load_test_filename)).getText().toString();
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_FILENAME, filename);
        resultIntent.putExtra(EXTRA_CENTER_NAME, centerName);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();

    }
}
