package com.muzima.adapters.patients;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.muzima.utils.Constants;
import com.muzima.view.fragments.observations.ObservationsListingFragment;
import com.muzima.view.fragments.patient.HistoricalDataByTypeFragment;

public class HistoricalDataViewPagerAdapter extends FragmentStateAdapter {
    private final String patientUuid;
    private final Integer totalTabs;

    public HistoricalDataViewPagerAdapter(@NonNull FragmentActivity fa, Integer totalTabs, String patientUuid) {
        super(fa);
        this.totalTabs = totalTabs;
        this.patientUuid = patientUuid;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new HistoricalDataByTypeFragment(patientUuid);
            case 1:
                return new ObservationsListingFragment(Constants.OBSERVATIONS_FILTER_CATEGORY.BY_ENCOUNTER, patientUuid,false);
//            case 2:
//                return new ObservationsListingFragment(Constants.OBSERVATIONS_FILTER_CATEGORY.BY_ABSTRACT, patientUuid,false);
        }
        throw new AssertionError("This should never happen, let us blame the android operating system");
    }

    @Override
    public int getItemCount() {
        return totalTabs;
    }
}
