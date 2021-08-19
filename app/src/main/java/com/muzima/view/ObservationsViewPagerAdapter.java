package com.muzima.view;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.muzima.R;
import com.muzima.utils.Constants;
import com.muzima.view.fragments.observations.ObservationsListingFragment;

import org.jetbrains.annotations.NotNull;

public class ObservationsViewPagerAdapter extends FragmentPagerAdapter {
    private Context context;
    private String patientUuid;

    public ObservationsViewPagerAdapter(@NonNull @NotNull FragmentManager fm, Context context, String patientUuid) {
        super(fm);
        this.context = context;
        this.patientUuid = patientUuid;
    }

    @NonNull
    @NotNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new ObservationsListingFragment(Constants.OBSERVATIONS_FILTER_CATEGORY.BY_TYPE, patientUuid,false);
            case 1:
                return new ObservationsListingFragment(Constants.OBSERVATIONS_FILTER_CATEGORY.BY_ABSTRACT, patientUuid,false);
            case 2:
                return new ObservationsListingFragment(Constants.OBSERVATIONS_FILTER_CATEGORY.BY_ENCOUNTER, patientUuid,false);
        }
        throw new AssertionError("This should never happen, let us blame the android operating system");
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getResources().getString(R.string.general_by_type_title);
            case 1:
                return context.getResources().getString(R.string.general_by_abstract_title);
            case 2:
                return context.getResources().getString(R.string.general_by_encounter_title);
        }
        throw new AssertionError("This should never happen, let us blame the android operating system");
    }

    @Override
    public int getCount() {
        return 3;
    }
}
