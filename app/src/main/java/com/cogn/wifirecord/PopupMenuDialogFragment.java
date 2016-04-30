package com.cogn.wifirecord;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.Iterator;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PopupMenuDialogFragment.OptionSetListener} interface
 * to handle interaction events.
 */
public class PopupMenuDialogFragment extends DialogFragment implements View.OnClickListener {

    public interface OptionSetListener {
        public void onOptionSet(Bundle results);
    }

    private OptionSetListener mListener;

    public PopupMenuDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        LinearLayout linLayout=new LinearLayout(getActivity());
        linLayout.setOrientation(LinearLayout.VERTICAL);

        Iterator<String> options = getArguments().getStringArrayList("options").iterator();

        while (options.hasNext())
        {
            Button b1 = new Button(getActivity());
            b1.setText(options.next());
            b1.setPadding(10, 10, 10, 10);
            b1.setOnClickListener(this);
            linLayout.addView(b1);
        }

        return linLayout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OptionSetListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OptionSetListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {
        Bundle results = new Bundle(getArguments());
        String value = ((Button)view).getText().toString();
        results.putString("value", value);
        mListener.onOptionSet(results);
    }

}
