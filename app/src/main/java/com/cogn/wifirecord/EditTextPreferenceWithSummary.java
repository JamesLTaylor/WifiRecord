package com.cogn.wifirecord;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * Created by James on 5/16/2016.
 */
public class EditTextPreferenceWithSummary extends EditTextPreference {
    public String summary_format;

    public EditTextPreferenceWithSummary(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.EditTextPreferenceWithSummary,
                0, 0);

        try {
            summary_format = a.getString(R.styleable.EditTextPreferenceWithSummary_summary_format);
        } finally {
            a.recycle();
        }
    }

    public EditTextPreferenceWithSummary(Context context) {
        super(context);
    }



}
