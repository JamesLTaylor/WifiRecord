package com.cogn.wifirecord;

import android.os.Environment;

import java.text.SimpleDateFormat;

public class DataReadWrite {
    public static final SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    public static final String BaseFolder = Environment.getExternalStorageDirectory() + "/WifiRecord";
}
