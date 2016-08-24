package com.example.lg.stockmarketviewer;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * A simple {@link Fragment} subclass.
 */
public class Historic extends Fragment {

    private String company_name = "";
    private View rootView = null;
    private ProgressDialog progress = null;
    public Historic() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_historic, container, false);
        Bundle b = getArguments();
        if(b != null)
        {
            progress = ProgressDialog.show(getContext(), "Get data", "Please wait...");
            company_name = b.getString("company");
            String url = "http://highchart-1283.appspot.com/?symbol="+company_name;
            WebView v = (WebView)rootView.findViewById(R.id.highchart);
            WebSettings settings = v.getSettings();
            settings.setJavaScriptEnabled(true);
            v.loadUrl(url);
            if(progress.isShowing())
                progress.dismiss();
        }
        return rootView;
    }
}
