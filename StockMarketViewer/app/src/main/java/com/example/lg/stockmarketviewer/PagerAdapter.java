package com.example.lg.stockmarketviewer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by LG on 2016/4/13.
 */
public class PagerAdapter extends FragmentStatePagerAdapter{
    private Bundle parameters;
    public PagerAdapter(FragmentManager fm, Bundle b)
    {
        super(fm);
        parameters = b;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment frag = null;
        switch(position)
        {
            case 0:
                frag = new Current();
                frag.setArguments(parameters);
                break;
            case 1:
                frag = new Historic();
                frag.setArguments(parameters);
                break;
            case 2:
                frag = new News();
                frag.setArguments(parameters);
                break;
        }
        return frag;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String title = "";
        switch (position){
            case 0:
                title = "CURRENT";
                break;
            case 1:
                title = "HISTORIC";
                break;
            case 2:
                title = "NEWS";
                break;
        }
        return title;
    }
}
