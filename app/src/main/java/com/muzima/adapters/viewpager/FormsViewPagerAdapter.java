package com.muzima.adapters.viewpager;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.muzima.R;
import com.muzima.view.fragments.forms.AllFormsListFragment;
import com.muzima.view.fragments.forms.AvailableFormsFragment;
import com.muzima.view.fragments.forms.DownloadedFormsFragment;

public class FormsViewPagerAdapter extends FragmentPagerAdapter {

    private Context context;

    public FormsViewPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0){
            return new AllFormsListFragment();
        }else if (position == 1){
            return new DownloadedFormsFragment();
        }else if (position == 2){
            return new AvailableFormsFragment();
        }else
            return null;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        if (position == 0){
            return context.getString(R.string.general_all_forms);
        }else if (position == 1){
            return context.getString(R.string.general_downloaded);
        }else if (position == 2){
            return context.getString(R.string.general_available_online);
        }else
            return null;
    }

    @Override
    public int getCount() {
        return 3;
    }
}
