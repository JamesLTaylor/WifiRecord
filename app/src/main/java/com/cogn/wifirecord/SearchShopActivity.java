package com.cogn.wifirecord;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.Arrays;

public class SearchShopActivity extends Activity implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    public static final String INTENT_EXTRA_SHOP = "com.cogn.wifirecord.INTENT_EXTRA_SHOP";
    public static final String INTENT_EXTRA_CATEGORY = "com.cogn.wifirecord.INTENT_EXTRA_CATEGORY";
    public static final String INTENT_EXTRA_DIRECTIONS = "com.cogn.wifirecord.INTENT_EXTRA_DIRECTIONS";
    private Spinner categorySpinner;
    private Spinner shopNameSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_shop);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent myIntent = getIntent();
        //String location = myIntent.getStringExtra("location");

        categorySpinner = (Spinner) findViewById(R.id.shop_search_category);
        shopNameSpinner = (Spinner) findViewById(R.id.shop_search_shopname);

        categorySpinner.setOnItemSelectedListener(this);
        shopNameSpinner.setOnItemSelectedListener(this);

        String[] categoryArray = GlobalDataFragment.shopDirectory.listCategories();
        Arrays.sort(categoryArray);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categoryArray);
        categorySpinner.setAdapter(adapter);

        findViewById(R.id.shop_search_btn_show).setOnClickListener(this);
        findViewById(R.id.shop_search_btn_directions).setOnClickListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == categorySpinner.getId()){
            String selectedCategory = categorySpinner.getSelectedItem().toString();
            String[] shopNameArray = GlobalDataFragment.shopDirectory.listShopNames(selectedCategory);
            Arrays.sort(shopNameArray);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, shopNameArray);
            shopNameSpinner.setAdapter(adapter);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onClick(View v) {
        boolean directions = (v.getId() == R.id.shop_search_btn_directions);
        String category = categorySpinner.getSelectedItem().toString();
        String shopName = shopNameSpinner.getSelectedItem().toString();
        Intent resultIntent = new Intent();
        resultIntent.putExtra(INTENT_EXTRA_DIRECTIONS, directions);
        resultIntent.putExtra(INTENT_EXTRA_CATEGORY, category);
        resultIntent.putExtra(INTENT_EXTRA_SHOP, shopName);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}
