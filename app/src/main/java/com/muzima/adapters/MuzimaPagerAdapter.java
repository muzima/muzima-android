/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.muzima.view.MuzimaListFragment;

public abstract class MuzimaPagerAdapter extends FragmentPagerAdapter{
    protected PagerView[] pagers;
    protected final Context context;

    protected MuzimaPagerAdapter(Context context, FragmentManager fm) {
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

    public void reloadData() {
        for (PagerView pager : pagers) {
            pager.fragment.reloadData();
        }
    }

    public static class PagerView {
        final String title;
        public final MuzimaListFragment fragment;

        public PagerView(String title, MuzimaListFragment fragment) {
            this.title = title;
            this.fragment = fragment;



        }
    }
}
