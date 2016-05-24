package com.cogn.wifirecord;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.SparseArray;

import java.io.InputStream;
import java.util.List;


public class WifiScanner implements ProvidesWifiScan {
    private WifiManager wifiManager;
    private MacLookup macLookup;

    public WifiScanner(WifiManager wifiManager, InputStream macsInputStream){
        this.wifiManager = wifiManager;
        this.macLookup = new MacLookup(macsInputStream);
    }
    @Override
    public SparseArray<Float> getScanResults(long atTime) {
        SparseArray<Float> result = new SparseArray<>();
        List<ScanResult> scanned = wifiManager.getScanResults();
        for (ScanResult scan : scanned) {
            int macID = macLookup.GetId(scan.BSSID, scan.SSID);
            result.put(macID, (float)scan.level);
        }
        wifiManager.startScan();
        return result;

    }
}
