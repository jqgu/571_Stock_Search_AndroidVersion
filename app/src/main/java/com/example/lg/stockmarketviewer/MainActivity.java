package com.example.lg.stockmarketviewer;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private String raw_data = "";
    private String name = "";
    private ActionBar bar = null;
    private int oncreate_show = 0;
    private int flag = 0;
    private  String symbol_list = "";
    private  MyAdapter adp = null;
    private String []item_list = null;
    private int current_pos = 0;
    private  int total = 0;
    private ListView favourite_list = null;
    private AutoCompleteTextView au = null;
    private ArrayAdapter au_adapter = null;
    private  ArrayList<String> suggestion_list = new ArrayList<>();
    private ArrayList<Map<String, Object>> list = new ArrayList<>();
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private ProgressDialog progress = null;
    private Handler handler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            if(msg.what == 0x123)
            {
                check(raw_data);
            }
            else if(msg.what == 0x11)
            {
                Log.d("bbbbb","tt send message");
                current_pos++;
                parse(current_pos, 0);
            }
            else if(msg.what == 0x12)
            {
                Log.d("bbbbb","refresh tt send message");
                current_pos++;
                parse(current_pos, 1);
            }
            else if(msg.what == 0x13)
            {
                Log.d("bbbbb","receive error 0x13");
                current_pos++;
                if(current_pos != total)
                    require_data(item_list[current_pos], 0);
                else
                    adp.notifyDataSetChanged();
            }
            else if(msg.what == 0x14)
            {
                Log.d("bbbbb","receive refresh error 0x14");
                current_pos++;
                if(current_pos != total)
                    require_data(item_list[current_pos], 1);
                else
                {
                    adp.notifyDataSetChanged();
                    findViewById(R.id.refresh).setClickable(true);
                    Toast.makeText(getApplicationContext(), "refresh finish", Toast.LENGTH_SHORT).show();
                }
            }
            else if(msg.what == 0x200)
            {
                autocomplete_list();
            }
        }
    };
    private Handler auto_handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //Toast.makeText(getApplicationContext(), "runnable", Toast.LENGTH_SHORT).show();
            start_refresh();
            auto_handler.postDelayed(this, 10000);
        }
    };
    private GestureDetector detector = new GestureDetector(new OnGestureListener()
    {
        @Override
        public boolean onDown(MotionEvent e) {
            Log.d("ddddd","Down");
            return false;
        }
        @Override
        public void onShowPress(MotionEvent e) {
            Log.d("ddddd","ShowPress");
        }
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.d("ddddd","SingleTapUp");
            return false;
        }
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }
        @Override
        public void onLongPress(MotionEvent e) {
            Log.d("ddddd","Long Press");
        }
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if(e1.getX() - e2.getX() > 50)
            {
                final int selected_one = favourite_list.pointToPosition((int) e1.getX(), (int) e1.getY());
                final String comp_name = (String)list.get(selected_one).get("fav_name");
                final String comp_symbol = (String)list.get(selected_one).get("fav_symbol");
                Log.d("ddddd", "Flipping No." + selected_one);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Want to delete "+ comp_name +" from favourites?");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        list.remove(selected_one);
                        adp.notifyDataSetChanged();
                        symbol_list = preferences.getString("symbol_list", "");
                        int pos = symbol_list.indexOf(comp_symbol+";");
                        if(symbol_list.equals("") == false && pos != -1)
                        {
                            symbol_list = symbol_list.substring(0, pos)+symbol_list.substring(pos+comp_symbol.length()+1);
                        }
                        editor.putString("symbol_list", symbol_list);
                        editor.commit();
                        Log.d("ddddd", "you click OK");
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("ddddd", "you click Cancel");
                    }
                });
                builder.show();
            }

            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bar = getSupportActionBar();
        bar.setLogo(R.drawable.stock);
        bar.setDisplayUseLogoEnabled(true);
        bar.setDisplayShowHomeEnabled(true);
        preferences = getSharedPreferences("favourite", MODE_PRIVATE);
        editor = preferences.edit();

        favourite_list = (ListView)findViewById(R.id.fav_list);
        favourite_list.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                detector.onTouchEvent(event);
                return false;
            }
        });
        String str = preferences.getString("symbol_list","Nothing");
        oncreate_show = 1;
        adp = new MyAdapter(this, list, R.layout.fav_list_item, new String[]{"fav_symbol", "fav_name", "fav_price", "fav_percent", "fav_market"}, new int[]{R.id.fav_symbol, R.id.fav_name, R.id.fav_price, R.id.fav_percent, R.id.fav_market});
        favourite_list.setAdapter(adp);
        if(str.equals("Nothing") == false && str.equals("") == false)
        {
            Log.d("aaaaa",str);
            show_favourite_list(str, 0);
        }
        Switch auto = (Switch)findViewById(R.id.auto_refresh);
        auto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    start_auto_refresh();
                else
                    stop_auto_refresh();
            }
        });

        au = (AutoCompleteTextView)findViewById(R.id.search);
        au.setThreshold(3);
        au.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String text = ((TextView) view.findViewById(R.id.an_item)).getText().toString();
                int pos = text.indexOf('\n');
                text = text.substring(0, pos);
                au.setText(text);
                Editable etext = au.getText();
                Selection.setSelection(etext, etext.length());
            }
        });
        au.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) {
                    if ((s.toString()).indexOf('(') != -1)
                        return;
                    get_suggestion(s.toString());
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void get_suggestion(String s) {
        suggestion_list.clear();
        final String location = "http://dev.markitondemand.com/MODApis/Api/v2/Lookup/json?input=" + s;
        Log.d("aaaaa", "get data url: " + location);
        new Thread(){
            public void run()
            {
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
                    Log.d("aaaaa", "finish! :"+raw_data);
                    handler.sendEmptyMessage(0x200);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("aaaaa", "raise an exception");
                }
            }
        }.start();
    }

    private void autocomplete_list() {
        JSONArray jarray = null;
        try {
            jarray = new JSONArray(raw_data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject an_item = null;
        String symbol = "";
        String name = "";
        String exchange = "";
        for(int i = 0; i < jarray.length(); i++)
        {
            try {
                an_item = jarray.getJSONObject(i);
                symbol = an_item.getString("Symbol");
                name = an_item.getString("Name");
                exchange = an_item.getString("Exchange");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String str = symbol+"\n"+name+" ("+exchange+")";
            suggestion_list.add(str);
        }
        au_adapter = new ArrayAdapter(this, R.layout.suggestion_item, R.id.an_item, suggestion_list.toArray());
        au.setAdapter(au_adapter);
    }

    public void get_quote(View source)
    {
        name = ((AutoCompleteTextView)findViewById(R.id.search)).getText().toString();
        if(name.equals(""))
        {
            new AlertDialog.Builder(this).setMessage("Please Enter a Stock Name/Symbol").setPositiveButton("OK", null).show();
        }
        else
        {
            require(name);
        }
    }

    private void require(String name)
    {
        final String location = "http://dev.markitondemand.com/MODApis/Api/v2/Quote/json?symbol="+name;
        new Thread()
        {
            public void run()
            {
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
                    handler.sendEmptyMessage(0x123);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void check(String data)
    {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(data);
            if(jsonObject.has("Message"))
            {
                new AlertDialog.Builder(this).setMessage("Invalid Symbol").setPositiveButton("OK", null).show();
            }
            else if(jsonObject.getString("Status").contains("Failure"))
            {
                new AlertDialog.Builder(this).setMessage("Invalid Symbol").setPositiveButton("OK", null).show();
            }
            else
            {
                oncreate_show = 0;
                Bundle b = new Bundle();
                b.putString("company",name);
                b.putString("jsondata",data);
                Intent stock_details = new Intent(MainActivity.this, Stock_Details.class);
                stock_details.putExtras(b);
                startActivity(stock_details);
            }
        } catch (JSONException e) {
            //Log.d("here", "exception: "+e.toString());
            e.printStackTrace();
        }
    }
    public void clear_info(View source)
    {
        AutoCompleteTextView au = (AutoCompleteTextView)findViewById(R.id.search);
        au.setText("");
    }

    private void start_auto_refresh()
    {
        auto_handler.postDelayed(runnable, 10000);
    }
    private void stop_auto_refresh()
    {
        auto_handler.removeCallbacks(runnable);
    }
    @Override
    public void onStart()
    {
        super.onStart();
        symbol_list = preferences.getString("symbol_list","Nothing");
        Log.d("aaaaa", "symbol list: " + symbol_list);
        if(symbol_list.equals("Nothing") == false && oncreate_show == 0)
            update_favourite_list();
    }
    private void update_favourite_list()
    {
        String []items = symbol_list.split(";");
        if(symbol_list.equals("") && list.size() == 1)
        {
            list.remove(0);
            adp.notifyDataSetChanged();
            return;
        }
        if(symbol_list.equals("") && list.size() == 0)
            return;
        if(items.length == list.size())
            return;
        Log.d("aaaaa", "update symbol list: " + symbol_list+", list.size= "+list.size());
        Map<String, Object> mmap = null;
        int fflag = 1;
        String new_one = "";
        for(int i = 0; i < list.size(); i++)
        {
            fflag = 0;
            mmap = list.get(i);
            String sname = (String)mmap.get("fav_symbol");
            for(int j = 0; j < items.length; j++)
            {
                if(items[j].equals(sname) == true)
                {
                    fflag = 1;
                }
            }
            if(fflag == 0)
            {
                list.remove(i);
                break;
            }
        }
        if(fflag == 0)
        {
            adp.notifyDataSetChanged();
            boolean tt = ((ListView)findViewById(R.id.fav_list)).getAdapter().equals(adp);
            Log.d("aaaaa", "update remove!!! adapter match:"+tt);
            return;
        }
        fflag = 0;
        for(int i = 0; i < items.length; i++)
        {
            fflag = 0;
            for(int j = 0; j < list.size(); j++)
            {
                mmap = list.get(j);
                if(items[i].equals(mmap.get("fav_symbol")))
                {
                    fflag = 1;
                }
            }
            if(fflag == 0)
            {
                new_one = items[i];
                break;
            }
        }
        final String location = "http://dev.markitondemand.com/MODApis/Api/v2/Quote/json?symbol="+new_one;
        Thread find_new_one = new Thread(new Runnable() {
            @Override
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        find_new_one.start();
        try {
            find_new_one.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String symbol = "";
        String name = "";
        String price = "";
        String changePercent = "";
        String marketCap = "";
        try {
            JSONObject jobject = new JSONObject(raw_data);
            symbol = jobject.getString("Symbol");
            name = jobject.getString("Name");
            price = jobject.getString("LastPrice");
            changePercent = jobject.getString("ChangePercent");
            marketCap = jobject.getString("MarketCap");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        DecimalFormat df = new DecimalFormat("0.00");
        Log.d("aaaaa","changepercent:  " + changePercent);
        changePercent = df.format(Double.valueOf(changePercent));
        if(Double.valueOf(changePercent) > 0)
            changePercent = "+"+changePercent;
        double cap = Double.valueOf(marketCap);
        if(cap > 1000000000.0)
        {
            cap /= 1000000000.0;
            marketCap = df.format(cap)+" Billion";
        }
        else if(cap > 1000000.0)
        {
            cap /= 1000000.0;
            marketCap = df.format(cap)+" Million";
        }
        mmap = new HashMap<>();
        mmap.put("fav_symbol", symbol);
        mmap.put("fav_name", name);
        mmap.put("fav_price", "$"+price);
        mmap.put("fav_percent", changePercent+"%");
        mmap.put("fav_market", "Market Cap:"+marketCap);
        list.add(mmap);
        adp.notifyDataSetChanged();
        boolean tt = ((ListView)findViewById(R.id.fav_list)).getAdapter().equals((ListAdapter)adp);
        Log.d("aaaaa", "update add!!! adapter match:" + tt);
        //Log.d("aaaaa", "here??");
    }

    class MyAdapter extends SimpleAdapter
    {
        public MyAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }
        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {

            View v = super.getView(pos, convertView, parent);

            TextView ss = (TextView)v.findViewById(R.id.fav_symbol);
            ss.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    name = ((TextView)v).getText().toString();
                    require(name);
                }
            });

            TextView t = (TextView) v.findViewById(R.id.fav_percent);
            String value = t.getText().toString();
            if(value.charAt(0) == '+')
                t.setBackgroundColor(Color.parseColor("#00ff00"));
            else if(value.charAt(0) == '-')
                t.setBackgroundColor(Color.parseColor("#ff0000"));
            return v;
        }
    }
    private void show_favourite_list(String data, int type)
    {
        progress = ProgressDialog.show(this,"Get data", "Please wait...");
        item_list = data.split(";");
        current_pos = 0;
        total = item_list.length;
        require_data(item_list[current_pos], type);
    }

    private void require_data(String para, int type)
    {
        final String location = "http://dev.markitondemand.com/MODApis/Api/v2/Quote/json?symbol="+para;
        final int ttype = type;
        raw_data = "";
        Log.d("bbbbb", Thread.currentThread().getName() + ":" + location);
        Thread tt = new Thread(new Runnable() {
            @Override
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
                        Log.d("bbbbb",Thread.currentThread().getName()+":reading");
                    }
                    bufferedReader.close();
                    reader.close();
                    connection.disconnect();
                    raw_data = sb.toString();
                    if(ttype == 0)           //construct the whole list
                        handler.sendEmptyMessage(0x11);
                    else if(ttype == 1)   //refresh list
                        handler.sendEmptyMessage(0x12);
                } catch (Exception e) {
                    Log.d("bbbbb", "catch exception, skip it");

                    if(ttype == 0)
                        handler.sendEmptyMessage(0x13);
                    else if(ttype == 1)
                        handler.sendEmptyMessage(0x14);
                    e.printStackTrace();

                }
            }
        });
        tt.setName("tt");
        tt.start();
    }

    private void parse(int pos, int type)
    {
        String symbol = "";
        String name = "";
        String price = "";
        String changePercent = "";
        String marketCap = "";
        HashMap<String, Object> map = new HashMap<>();
        try {
            JSONObject jobject = new JSONObject(raw_data);
            symbol = jobject.getString("Symbol");
            name = jobject.getString("Name");
            price = jobject.getString("LastPrice");
            changePercent = jobject.getString("ChangePercent");
            marketCap = jobject.getString("MarketCap");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        DecimalFormat df = new DecimalFormat("0.00");
        changePercent = df.format(Double.valueOf(changePercent));
        if(Double.valueOf(changePercent) > 0)
            changePercent = "+"+changePercent;
        double cap = Double.valueOf(marketCap);
        if(cap > 1000000000.0)
        {
            cap /= 1000000000.0;
            marketCap = df.format(cap)+" Billion";
        }
        else if(cap > 1000000.0)
        {
            cap /= 1000000.0;
            marketCap = df.format(cap)+" Million";
        }
        if(type == 0)
        {
            map.put("fav_symbol", symbol);
            map.put("fav_name", name);
            map.put("fav_price", "$"+price);
            map.put("fav_percent", changePercent+"%");
            map.put("fav_market", "Market Cap:" + marketCap);
            list.add(map);
        }
        else if(type == 1)
        {
            Map<String, Object> ii = list.get(current_pos-1);
            if(ii.get("fav_price").equals("$"+price) == false)
                ii.put("fav_price","$"+price);
            if(ii.get("fav_percent").equals(changePercent+"%") == false)
                ii.put("fav_percent", changePercent+"%");
        }

        if(pos == total)
        {
            current_pos = 0;
            total = 0;
            adp.notifyDataSetChanged();
            (findViewById(R.id.refresh)).setClickable(true);
            if(flag == 1)
            {Toast.makeText(this,"refresh finish",Toast.LENGTH_SHORT).show();}
            if(progress.isShowing())
            {
                progress.dismiss();
            }

        }
        else
            require_data(item_list[current_pos], type);
    }

    public void refresh(View v)
    {
        v.setClickable(false);
        start_refresh();
    }

    private void start_refresh()
    {
        symbol_list = preferences.getString("symbol_list", "Nothing");
        if(symbol_list.equals("") || symbol_list.equals("Nothing"))
            return;
        flag = 1;
        show_favourite_list(symbol_list, 1);
    }
}
