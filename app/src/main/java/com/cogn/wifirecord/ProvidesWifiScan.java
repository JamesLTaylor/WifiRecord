package com.cogn.wifirecord;

import android.util.SparseArray;

public interface ProvidesWifiScan {
    SparseArray<Float> getScanResults(long atTime);
}
