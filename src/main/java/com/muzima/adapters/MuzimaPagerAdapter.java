package com.muzima.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.muzima.view.MuzimaListFragment;

public abstract class MuzimaPagerAdapter extends FragmentPagerAdapter{
    protected PagerView[] pagers;
    protected Context context;

    public MuzimaPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.context = context;
        initPagerViews();
    }

    @Override
    public Fragment getItem(int position) {
        return pagers[position].fragment;
    }

    @Override
    public int getCount() {
        return pagers.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return pagers[position].title;
    }

    public abstract void initPagerViews();

    public static class PagerView {
        public String title;
        public MuzimaListFragment fragment;

        public PagerView(String title, MuzimaListFragment fragment) {
            this.title = title;
            this.fragment = fragment;
        }
    }
}
