package com.cogn.wifirecord;

import android.app.Fragment;
import android.os.Bundle;

/**
 * Objects used by all activities
 */
public class GlobalDataFragment extends Fragment {
    public static ShopDirectory shopDirectory;
    public static Graph mallGraph;
    public static OfflineWifiScanner offlineWifiScanner;
    public static StoredLocationInfo storedLocationInfo;
    public static RecordForLocation locator = null;
    public static boolean continuousLocate = false;
    public static Route latestRoute = null;

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }
}
