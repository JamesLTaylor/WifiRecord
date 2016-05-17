package com.cogn.wifirecord;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.SparseArray;

import java.io.InputStream;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;

@RunWith(AndroidJUnit4.class)
public class RecordActivityTest {

    @Rule
    public ActivityTestRule<RecordActivity> mActivityRule = new ActivityTestRule<>(
            RecordActivity.class);

    @Test
    public void offlineWalkthoughTest() {
        // Type text and then press the button.
        //ViewMatchers.
        //onView(ViewMatchers.withId(R.id.recordLayout)).perform(click());
        final RecordActivity record = mActivityRule.getActivity();
        //InputStream inputStream = record.getBaseContext().getResources().openRawResource(R.raw.home_continuous_20160515_070226);
        InputStream inputStream = record.getBaseContext().getResources().openRawResource(R.raw.greenstone_continuous_20160511_130351);
        final OfflineWifiScanner offlineWifiProvider = new OfflineWifiScanner(inputStream);
        long totalRecordingTime = offlineWifiProvider.getTotalRecordingTime();
        SparseArray<Float> scan = offlineWifiProvider.getScanResults(0);
        record.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //record.SetLocation("Home");
                record.SetLocation("Greenstone");
                record.StartLocating(offlineWifiProvider);
            }
        });
        try {
            Thread.sleep(totalRecordingTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }
}
