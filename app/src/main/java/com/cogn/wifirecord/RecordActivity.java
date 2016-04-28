package com.cogn.wifirecord;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;


public class RecordActivity extends Activity
    implements LocationFragment.LocationSetListener {

    private LocationFragment locationMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        LinearLayout myLayout = (LinearLayout)findViewById(R.id.recordLayout);

        ScrollImageView floorMapView = new ScrollImageView(this);
        Bitmap floorMapImage = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.greenstone_upper);
        floorMapView.setImage(floorMapImage);

        floorMapView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        myLayout.addView(floorMapView);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.record, menu);
        return true;
    }

    public void recordClick(View view) {
        /*TextView results = (TextView)findViewById(R.id.recordResults);
        results.append("\n\nscanning...\n");

        WifiManager manager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        manager.startScan();
        List<ScanResult> scanned = manager.getScanResults();
        for (ScanResult scan : scanned) {
            results.append(scan.SSID+"\n");
            results.append(scan.BSSID+"\n");
            results.append(scan.level+"\n");
        }*/
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.menu_select_location:
                View view = findViewById(R.id.recordLayout);
                locationMenu = new LocationFragment();
                locationMenu.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
                locationMenu.show(getFragmentManager(),"Diag");

                //PopupMenu popupMenu = new PopupMenu(getApplicationContext(), view, Gravity.CENTER);
                //popupMenu.inflate(R.menu.select_location);
                //popupMenu.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
                //return true;

        }

    }

    @Override
    public void onLocationSet(String newLocation) {
        locationMenu.dismiss();
    }
}
