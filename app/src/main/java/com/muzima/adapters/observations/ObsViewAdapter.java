package com.muzima.adapters.observations;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.muzima.view.fragments.patient.ChronologicalObsViewFragment;

public class ObsViewAdapter extends FragmentPagerAdapter {
    private final String patientUuid;
    private final Integer totalTabs;

    public ObsViewAdapter(@NonNull FragmentManager fm, Integer totalTabs, String patientUuid) {
        super(fm, BEHAVIOR_SET_USER_VISIBLE_HINT);
        this.totalTabs = totalTabs;
        this.patientUuid = patientUuid;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
//        if (position == 1)
//                return new TabularObsViewFragment(patientUuid);
//            else
                return new ChronologicalObsViewFragment(patientUuid);
    }

    @Override
    public int getCount() {
        return totalTabs;
    }
}