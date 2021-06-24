package com.muzima.adapters.viewpager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.muzima.R;
import com.muzima.view.fragments.GuidedSetupImageCardFragment;

public class GuidedSetupCardsViewPagerAdapter extends FragmentPagerAdapter {
    private Context context;

    public GuidedSetupCardsViewPagerAdapter(@NonNull FragmentManager fm,Context context) {
        super(fm);
        this.context = context;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new GuidedSetupImageCardFragment(R.drawable.security, context.getResources().getString(R.string.general_security), context.getResources().getString(R.string.general_security_description));
            case 1:
                return new GuidedSetupImageCardFragment(R.drawable.multiple_use_cases, context.getResources().getString(R.string.general_multiple_use_cases), context.getResources().getString(R.string.general_multiple_cases_description));
            case 2:
                return new GuidedSetupImageCardFragment(R.drawable.openmrs_compatibility, context.getResources().getString(R.string.general_openmrs_compatibility), context.getResources().getString(R.string.general_openmrs_compatibility_description));
        }
        return null;
    }

    @Override
    public int getCount() {
        return 3;
    }
}
