package com.muzima.adapters.viewpager;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.muzima.R;
import com.muzima.view.forms.AllAvailableFormsListFragment;
import com.muzima.view.forms.CompleteFormsListFragment;
import com.muzima.view.forms.IncompleteFormsListFragment;

public class FormsViewPagerAdapter extends FragmentPagerAdapter {

    private Context context;

    public FormsViewPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0){
            return new AllAvailableFormsListFragment();
        }else if (position == 1){
            return new IncompleteFormsListFragment();
        }else if (position == 2){
            return new CompleteFormsListFragment();
        }else
            return null;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        if (position == 0){
            return context.getString(R.string.general_all_forms);
        }else if (position == 1){
            return context.getString(R.string.info_complete_form);
        }else if (position == 2){
            return context.getString(R.string.info_incomplete_form);
        }else
            return null;
    }

    @Override
    public int getCount() {
        return 3;
    }
}
