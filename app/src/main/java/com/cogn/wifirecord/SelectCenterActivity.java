package com.cogn.wifirecord;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class SelectCenterActivity extends Activity implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    public static final String INTENT_EXTRA_SELECTED_CENTER = "com.cogn.wifirecord.INTENT_EXTRA_SELECTED_CENTER";
    private Spinner spinnerCurrentCenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_center);

        spinnerCurrentCenter = (Spinner) findViewById(R.id.spinner_current_center);
        spinnerCurrentCenter.setOnItemSelectedListener(this);

        String[] categoryArray = ShoppingCenter.getListOfCenterNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categoryArray);
        spinnerCurrentCenter.setAdapter(adapter);

        findViewById(R.id.btn_change_center).setOnClickListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onClick(View v) {
        String selectedCenter = spinnerCurrentCenter.getSelectedItem().toString();
        Intent resultIntent = new Intent();
        resultIntent.putExtra(INTENT_EXTRA_SELECTED_CENTER, selectedCenter);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}
