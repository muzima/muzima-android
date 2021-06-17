package com.muzima.adapters.cohort;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.muzima.R;
import com.muzima.view.cohort.AllCohortsListFragment;
import com.muzima.view.cohort.AvailableCohortsFragment;
import com.muzima.view.cohort.DownloadedCohortsFragment;

public class CohortsViewPagerAdapter extends FragmentPagerAdapter {

    private Context context;

    public CohortsViewPagerAdapter(@NonNull FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new AllCohortsListFragment();
            case 1:
                return new DownloadedCohortsFragment();
            case 2:
                return new AvailableCohortsFragment();
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getResources().getString(R.string.general_display_all);
            case 1:
                return context.getResources().getString(R.string.general_downloaded);
            case 2:
                return context.getResources().getString(R.string.general_available_online);
            default:
                return "-";
        }
    }

    @Override
    public int getCount() {
        return 3;
    }
}
