package com.cogn.wifirecord;

import android.os.Bundle;
import android.app.Activity;

public class SearchShopActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_shop);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
