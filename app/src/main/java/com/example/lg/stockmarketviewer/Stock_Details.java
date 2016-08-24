package com.example.lg.stockmarketviewer;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.DecimalFormat;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;

public class Stock_Details extends AppCompatActivity {
    ViewPager pager;
    TabLayout tabLayout;
    ActionBar bar = null;
    String company_symbol = "";
    String company_name = "";
    String price = "";
    //String fav_string = "";
    String symbol = "";
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    ShareDialog shareDialog;
    CallbackManager callbackManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock__details);


        Bundle b = this.getIntent().getExtras();
        get_FB_info(b.getString("jsondata"));

        //b.putString("fav", fav_string);
        pager = (ViewPager) findViewById(R.id.view_pager);
        tabLayout = (TabLayout) findViewById(R.id.tab_layout);

        FragmentManager manager = getSupportFragmentManager();
        PagerAdapter adapter = new PagerAdapter(manager, b);
        pager.setAdapter(adapter);

        tabLayout.setupWithViewPager(pager);
        pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setTabsFromPagerAdapter(adapter);

        bar = getSupportActionBar();
        company_symbol = b.getString("company").toUpperCase();
        bar.setTitle(company_symbol);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(true);

        preferences = getSharedPreferences("favourite", MODE_PRIVATE);
        editor = preferences.edit();

        FacebookSdk.sdkInitialize(getApplicationContext());
        shareDialog = new ShareDialog(this);
        callbackManager = CallbackManager.Factory.create();
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {

            @Override
            public void onSuccess(Sharer.Result result) {
                Toast.makeText(getApplicationContext(), "You shared this post!", Toast.LENGTH_SHORT).show();
                Log.d("ABCDFB","POSTED!!");
            }

            @Override
            public void onCancel() {
                Toast.makeText(getApplicationContext(), "Post is cancelled!", Toast.LENGTH_SHORT).show();
                Log.d("ABCDFB", "Canceled!!");
            }

            @Override
            public void onError(FacebookException e) {
                Toast.makeText(getApplicationContext(), "error!", Toast.LENGTH_SHORT).show();
                Log.d("ABCDFB","Error!!");
            }
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void get_FB_info(String json_str)
    {
        if(json_str.equals("") || json_str == null)
        {
            Toast.makeText(this,"no fav string",Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject jobject = new JSONObject(json_str);
            price = jobject.getString("LastPrice");
            symbol = jobject.getString("Symbol");
            company_name = jobject.getString("Name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stock__details, menu);
        MenuItem item = menu.findItem(R.id.favourite);
        Log.d("tag", "" + (item == null));
        if(preferences.getString("symbol_list","").contains(symbol+";"))
        {
            item.setIcon(R.drawable.star);
            item.setTitle("star");
        }
        else
        {
            item.setIcon(R.drawable.empty_star);
            item.setTitle("empty");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Toast.makeText(this, "setting", Toast.LENGTH_SHORT).show();
            return true;
        }
        else if(id == R.id.favourite)
        {
            //Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
            if(item.getTitle().equals("empty"))
            {
                item.setIcon(R.drawable.star);
                item.setTitle("star");
                String symbol_list = preferences.getString("symbol_list", "");
                Log.d("aaaaa","before add one:"+symbol_list);
                symbol_list = symbol_list + symbol+";";
                editor.putString("symbol_list",symbol_list);
                editor.commit();
                Log.d("aaaaa", "after add one:" + symbol_list);
            }
            else
            {
                item.setIcon(R.drawable.empty_star);
                item.setTitle("empty");
                String symbol_list = preferences.getString("symbol_list", "");
                int index = symbol_list.indexOf(symbol+";");
                if(index != -1)
                {
                    symbol_list = symbol_list.substring(0, index)+symbol_list.substring(index+symbol.length()+1);
                }
                editor.putString("symbol_list", symbol_list);
                editor.commit();
                Log.d("aaaaa", "after remove one:" + symbol_list);
            }
            return true;
        }
        else if(id == R.id.fb)
        {
            //Toast.makeText(this, "Facebook sharing", Toast.LENGTH_SHORT).show();

            if (ShareDialog.canShow(ShareLinkContent.class)) {
                ShareLinkContent linkContent = new ShareLinkContent.Builder()
                        .setContentTitle("Current Stock Price of " + company_name + "," + price)
                        .setContentDescription(
                                "Stock Information of " + company_name)
                        .setContentUrl(Uri.parse("http://dev.markitondemand.com/MODApis/"))
                        .setImageUrl(Uri.parse("http://chart.finance.yahoo.com/t?lang=en-US&width=100&height=80&s=aapl"))
                        .build();
                shareDialog.show(linkContent);
            }
            return true;
        }
        else if(id == android.R.id.home)
        {
            this.finish();
            //Toast.makeText(this, "clicked", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
