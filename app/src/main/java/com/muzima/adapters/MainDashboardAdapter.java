package com.muzima.adapters;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.muzima.view.fragments.DashboardCohortsFragment;
import com.muzima.view.fragments.DashboardFormsFragment;
import com.muzima.view.fragments.DashboardHomeFragment;

public class MainDashboardAdapter extends FragmentPagerAdapter {

    public MainDashboardAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                return new DashboardHomeFragment();
            case 1:
                return new DashboardCohortsFragment();
            case 2:
                return new DashboardFormsFragment();
        }
        return null;
    }

    @Override
    public int getCount() {
        return 3;
    }
}
