package com.cogn.wifirecord;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ManualRecordActivity extends Activity implements View.OnClickListener {

    private long startTimeMillis;
    private WifiScanReceiver receiver;
    private int counter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_record);

        IntentFilter intentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        receiver = new WifiScanReceiver();
        registerReceiver(receiver, intentFilter);

        TextView view = (TextView)findViewById(R.id.manual_update_info);
        view.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        getBaseContext().unregisterReceiver(receiver);
        super.onDestroy();
    }

    public void UpdateResults(String value)
    {
        TextView view = (TextView)findViewById(R.id.manual_update_info);
        String newText = value+view.getText();
        view.setText(newText);
    }


    @Override
    public void onClick(View view) {
        counter = 0;
        WifiManager w = (WifiManager)view.getContext().getSystemService(Context.WIFI_SERVICE);
        boolean result = w.startScan();
        Log.d("MANUAL","***********************************************************\n");
        Log.d("MANUAL", "Scan request started:" + result +"\n");


        ((TextView)view).setText("***********************************************************\nScan request started:" + result +"\n");
        startTimeMillis = Calendar.getInstance().getTimeInMillis();

        new Timer().schedule(new LaunchScanTask(view.getContext()), 2000);

    }


    private class LaunchScanTask extends TimerTask
    {
        private final Context context;

        public LaunchScanTask(Context context)
        {this.context = context;}

        public void run()
        {
            WifiManager w = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            boolean result = w.startScan();
            Log.d("MANUAL", "Second scan request started:" + result);
        }
    }


    public class WifiScanReceiver extends BroadcastReceiver {
        private void WriteToUIThread(final String newText)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    UpdateResults(newText);
                }
            });
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MANUAL", "Scan received after: " + (Calendar.getInstance().getTimeInMillis() - startTimeMillis) + "ms");
            WriteToUIThread("\nScan received after: " + (Calendar.getInstance().getTimeInMillis() - startTimeMillis) + "ms\n\n");
            WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> scanned = wifiManager.getScanResults();

            for (ScanResult scan : scanned) {
                Log.d("MANUAL", scan.BSSID + "," + scan.SSID + "," + scan.level+"\n");
                WriteToUIThread(scan.BSSID + "," + scan.SSID + "," + scan.level+"\n");
            }



            /*if (counter<5)
            {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                boolean result = wifiManager.startScan();
                Log.d("MANUAL", "Started scan number:" + counter + ":" + result);
                counter+=1;
            }*/

        }
    }
}
