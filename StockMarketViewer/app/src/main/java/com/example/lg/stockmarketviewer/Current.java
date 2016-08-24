package com.example.lg.stockmarketviewer;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Inflater;

import uk.co.senab.photoview.PhotoViewAttacher;


/**
 * A simple {@link Fragment} subclass.
 */
public class Current extends Fragment {

    private String company_name = "";
    private String raw_data = "";
    private String fav_string = "";
    private ImageView show = null;
    private Bitmap bitmap = null;
    private  int flag_chart = 0;
    private  ProgressDialog progress = null;
    private Handler handler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            if(msg.what == 0x124)
            {
                pre_process();
            }
        }
    };
    private View rootView = null;
    private PhotoViewAttacher mAttacher;
    public Current() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_current, container, false);
        Bundle b = getArguments();
        if(b != null)
        {
            raw_data = b.getString("jsondata");
            company_name = b.getString("company");
            fav_string = b.getString("fav");
        }
        get_current_chart(company_name);
        return rootView;
    }

    private void get_current_chart(String name)
    {
        progress = ProgressDialog.show(getContext(), "Get Data", "Please wait...");
        final String location = "http://chart.finance.yahoo.com/t?lang=en-US&width=680&height=400&s="+name;
        new Thread()
        {
            public void run()
            {
                try {
                    URL url = new URL(location);
                    InputStream is = url.openStream();
                    bitmap = BitmapFactory.decodeStream(is);
                    flag_chart = 1;
                    handler.sendEmptyMessage(0x124);
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
    private void pre_process()
    {
        flag_chart = 0;
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        parse(raw_data, list);
        ListView lv = (ListView)rootView.findViewById(R.id.current_table);

        View header = getLayoutInflater(null).inflate(R.layout.current_caption, null);
        lv.addHeaderView(header);

        View footer = getLayoutInflater(null).inflate(R.layout.current_chart, null);
        ImageView pic = (ImageView) footer.findViewById(R.id.current_chart_picture);

        pic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getContext(), "you click on the chart", Toast.LENGTH_SHORT).show();
                ImageView newone = new ImageView(getContext());
                newone.setImageBitmap(bitmap);

                AlertDialog builder = new AlertDialog.Builder(getContext()).create();
                builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
                mAttacher = new PhotoViewAttacher(newone);

                builder.setView(newone);
                builder.show();
                builder.getWindow().setLayout(680,400);
            }
        });

        pic.setImageBitmap(bitmap);///////////////////////////
        lv.addFooterView(footer);

        SimpleAdapter adp = new SimpleAdapter(getContext(), list, R.layout.current_row_item, new String[]{"Key","Value","Arrow"}, new int[]{R.id.Key, R.id.Value, R.id.Arrow});
        lv.setAdapter(adp);
        if(progress.isShowing())
            progress.dismiss();
    }
    private void parse(String s, ArrayList<Map<String, Object>>list)
    {
        String name="";
        String symbol="";
        String lastprice="";
        String change="";
        String timestamp="";
        String marketcap="";
        String volume="";
        String changeYTD="";
        String high="";
        String low="";
        String open="";
        String [] value = {name,symbol,lastprice,change,timestamp,marketcap,volume,changeYTD,high,low,open};
        String [] key = {"Name", "Symbol","LastPrice","Change","Timestamp","MarketCap","Volume","ChangeYTD","High","Low","Open"};
        String change_percent = "";
        String change__percent_YTD = "";
        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(s);
            for(int i = 0; i < 11; i++)
            {
                value[i] = jsonObject.getString(key[i]);
            }
            change_percent = jsonObject.getString("ChangePercent");
            change__percent_YTD = jsonObject.getString("ChangePercentYTD");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        HashMap<String, Object> map=null;
        DecimalFormat df = new DecimalFormat("0.00");
        for(int i = 0; i < 11; i++)
        {
            //Log.d("parse",""+i+":"+value[i]);
            map = new HashMap<>();
            map.put("Key", key[i].toUpperCase());
            if(i == 3)
            {
                String prefix = "";
                value[i] = df.format(Double.valueOf(value[i]));
                change_percent = df.format(Double.valueOf(change_percent));
                if(Double.valueOf(change_percent) > 0)
                {
                    prefix = "+";
                    map.put("Arrow",R.drawable.up);
                }
                else if(Double.valueOf(change_percent) < 0)
                    map.put("Arrow",R.drawable.down);
                value[i] += "("+prefix+change_percent+"%)";
            }
            else if(i == 7)
            {
                String prefix = "";
                value[i] = df.format(Double.valueOf(value[i]));
                change__percent_YTD = df.format(Double.valueOf(change__percent_YTD));
                if(Double.valueOf(change__percent_YTD) > 0)
                {
                    prefix = "+";
                    map.put("Arrow",R.drawable.up);
                }
                else if(Double.valueOf(change__percent_YTD) < 0)
                    map.put("Arrow",R.drawable.down);
                value[i] += "("+prefix+change__percent_YTD+"%)";
            }
            else if(i == 4)
            {
                String []time = value[i].split(" ");
                value[i] = time[2]+" "+time[1]+" "+time[5]+", "+time[3];
            }
            else if(i == 5 || i == 6)
            {
                double cap = Double.valueOf(value[i]);
                if(cap > 1000000000.0)
                {
                    cap /= 1000000000.0;
                    value[i] = df.format(cap)+" Billion";
                }
                else if(cap > 1000000.0)
                {
                    cap /= 1000000.0;
                    value[i] = df.format(cap)+" Million";
                }
            }
            map.put("Value", value[i]);
            list.add(map);
        }
    }

}
