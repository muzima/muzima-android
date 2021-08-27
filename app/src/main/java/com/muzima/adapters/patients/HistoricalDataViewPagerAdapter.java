package com.muzima.adapters.patients;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import com.muzima.utils.Constants;
import com.muzima.view.fragments.observations.ObservationsListingFragment;
import org.jetbrains.annotations.NotNull;

public class HistoricalDataViewPagerAdapter extends FragmentPagerAdapter {
    private final String patientUuid;

    public HistoricalDataViewPagerAdapter(@NonNull @NotNull FragmentManager fm, String patientUuid) {
        super(fm, BEHAVIOR_SET_USER_VISIBLE_HINT);
        this.patientUuid = patientUuid;
    }

    @NonNull
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

    @Override
    public int getCount() {
        return 3;
    }
}
