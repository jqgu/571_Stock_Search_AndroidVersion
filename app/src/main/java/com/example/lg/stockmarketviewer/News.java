package com.example.lg.stockmarketviewer;


import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class News extends Fragment {

    private View rootView = null;
    private   String company_name = "";
    private  String raw_data = "";
    private  ArrayList<String> url_list;
    private   ProgressDialog progress = null;
    private   Handler handler = new Handler()
    {
      @Override
    public void handleMessage(Message msg)
      {
          if(msg.what == 0x126)
          {
              show_news();
          }
      }
    };

    public News() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView =  inflater.inflate(R.layout.fragment_news, container, false);
        Bundle b = getArguments();
        company_name = b.getString("company");
        get_news();
        return rootView;
    }

    private void get_news()
    {
        progress = ProgressDialog.show(getContext(), "Get data", "Please wait...");
        final String location = "http://highchart-1283.appspot.com/server.php?type=4&symbol=" + company_name;
        //Toast.makeText(getContext(), location, Toast.LENGTH_SHORT).show();
        new Thread() {
            public void run() {
                try {
                    URL url = new URL(location);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();
                    InputStream inputStream = connection.getInputStream();
                    Reader reader = new InputStreamReader(inputStream, "UTF-8");
                    BufferedReader bufferedReader = new BufferedReader(reader);
                    StringBuffer sb = new StringBuffer();
                    while ((raw_data = bufferedReader.readLine()) != null) {
                        sb.append(raw_data);
                    }
                    bufferedReader.close();
                    reader.close();
                    connection.disconnect();
                    raw_data = sb.toString();
                    handler.sendEmptyMessage(0x126);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
    private void show_news()
    {
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        parse(list);
        ListView lv = (ListView) rootView.findViewById(R.id.News_List);
        MyAdapter adp = new MyAdapter(getContext(), list, R.layout.news_item, new String[]{"news_title", "news_content", "news_publisher", "news_date"}, new int[]{R.id.news_title, R.id.news_content, R.id.news_publisher, R.id.news_date});
        lv.setAdapter(adp);
        if(progress.isShowing())
            progress.dismiss();
    }
    class MyAdapter extends SimpleAdapter {
        public MyAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            Log.d("lalala", "position: " + pos);
            View v = super.getView(pos, convertView, parent);

            TextView t = (TextView) v.findViewById(R.id.news_title);
            String s = "<a href=" + url_list.get(pos) + ">" + t.getText();
            t.setText(Html.fromHtml(s));
            t.setMovementMethod(LinkMovementMethod.getInstance());
            t.setLinkTextColor(Color.rgb(0, 0, 0));

            return v;
        }
    }

    private void parse(ArrayList<Map<String, Object>> list) {
        String title = "";
        String content = "";
        String publisher = "";
        String date = "";
        String url = "";
        Map<String, Object> map = null;
        JSONObject jsonObject = null;
        JSONArray news_array = null;
        JSONObject a_news = null;
        url_list = new ArrayList<>();
        try {
            jsonObject = new JSONObject(raw_data);

            news_array = (jsonObject.getJSONObject("d")).getJSONArray("results");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < news_array.length(); i++) {
            map = new HashMap<>();
            try {
                a_news = news_array.getJSONObject(i);
                url = a_news.getString("Url");
                url_list.add(url);
                title = a_news.getString("Title");
                content = a_news.getString("Description");
                publisher = "Publisher: " + a_news.getString("Source");
                date = "Date: " + get_date(a_news.getString("Date"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            map.put("news_title", title);
            map.put("news_content", content);
            map.put("news_publisher", publisher);
            map.put("news_date", date);
            list.add(map);
        }
    }

    private String get_date(String s) {
        String[] month = {
                "January", "February", "March",
                "April", "May", "June", "July",
                "August", "September", "October",
                "November", "December"
        };
        String date = "";
        String[] d1 = (s.split("T"))[0].split("-");
        String year = d1[0];
        String mon = month[Integer.valueOf(d1[1]) - 1];
        String day = d1[2];
        String time = (s.split("T"))[1];
        time = time.substring(0, time.length() - 1);
        date = day + " " + mon + " " + year + ", " + time;
        return date;
    }
}
