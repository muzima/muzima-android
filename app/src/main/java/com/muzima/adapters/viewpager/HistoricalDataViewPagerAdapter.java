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
import com.muzima.view.observations.ObservationsFragment;

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
                return new ObservationsFragment();
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
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 1;
    }
}
