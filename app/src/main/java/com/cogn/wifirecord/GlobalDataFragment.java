package com.cogn.wifirecord;

import android.app.Fragment;
import android.os.Bundle;

import java.util.Map;

/**
 * Objects used by all activities
 */
public class GlobalDataFragment extends Fragment {
    public static Map<String, String> centerNamesAndFolders;
    public static ShoppingCenter currentCenter;
    public static OfflineWifiScanner offlineWifiScanner;
    public static WifiFingerprintInfo wifiFingerprintInfo;
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
