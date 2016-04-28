package com.cogn.wifirecord;

import android.app.Activity;
import android.app.DialogFragment;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LocationFragment.LocationSetListener} interface
 * to handle interaction events.
 * Use the {@link LocationFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class LocationFragment extends DialogFragment implements View.OnClickListener {

    public interface LocationSetListener {
        public void onLocationSet(String newLocation);
    }

    private LocationSetListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LocationFragment.
     */
    public static LocationFragment newInstance(String param1, String param2) {
        LocationFragment fragment = new LocationFragment();
        return fragment;
    }
    public LocationFragment() {
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

        //return inflater.inflate(R.layout.fragment_location, container, false);
        LinearLayout linLayout=new LinearLayout(getActivity());
        linLayout.setOrientation(LinearLayout.VERTICAL);
        Button b1 = new Button(getActivity());
        b1.setText("Greenstone");
        b1.setPadding(10, 10, 10, 10);
        b1.setOnClickListener(this);
        linLayout.addView(b1);

        Button b2 = new Button(getActivity());
        b2.setText("Home");
        //b2.setTextSize();
        b2.setPadding(10, 10, 10, 10);
        b2.setOnClickListener(this);
        linLayout.addView(b2);

        return linLayout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (LocationSetListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement LocationSetListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {
        String locationName = ((Button)view).getText().toString();
        mListener.onLocationSet(locationName);
    }

}
