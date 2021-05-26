package com.muzima.adapters.viewpager;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.muzima.R;
import com.muzima.view.fragments.HistoricalDataByAbstractFragment;
import com.muzima.view.fragments.HistoricalDataByEncounterFragment;
import com.muzima.view.fragments.HistoricalDataByTypeFragment;

public class HistoricalDataViewPagerAdapter extends FragmentPagerAdapter {

    private Context context;

    public HistoricalDataViewPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                return new HistoricalDataByTypeFragment();
            case 1:
                return new HistoricalDataByAbstractFragment();
            case 2:
                return new HistoricalDataByEncounterFragment();
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position){
            case 0:
                return context.getResources().getString(R.string.info_by_type);
            case 1:
                return context.getResources().getString(R.string.info_by_abstract);
            case 2:
                return context.getResources().getString(R.string.info_by_encounter);
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 3;
    }
}
