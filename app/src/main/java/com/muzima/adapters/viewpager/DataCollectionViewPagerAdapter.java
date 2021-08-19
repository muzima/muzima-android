package com.muzima.adapters.viewpager;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.muzima.R;
import com.muzima.utils.Constants;
import com.muzima.view.fragments.AddSingleElementFragment;
import com.muzima.view.fragments.FillFormsFragment;
import com.muzima.view.fragments.observations.ObservationsListingFragment;

public class DataCollectionViewPagerAdapter extends FragmentPagerAdapter {

    private Context context;
    private String patientUuid;

    public DataCollectionViewPagerAdapter(FragmentManager fm, Context context, String patientUuid) {
        super(fm);
        this.context = context;
        this.patientUuid = patientUuid;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                return new FillFormsFragment(patientUuid);
            case 1:
                return  new ObservationsListingFragment(Constants.OBSERVATIONS_FILTER_CATEGORY.BY_TYPE, patientUuid,true);
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position){
            case 0:
                return context.getResources().getString(R.string.general_filling_forms);
            case 1:
                return  context.getResources().getString(R.string.general_add_single_element);
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }
}
