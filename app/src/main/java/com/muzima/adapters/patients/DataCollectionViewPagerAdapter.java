package com.muzima.adapters.patients;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import com.muzima.utils.Constants;
import com.muzima.view.fragments.AddSingleElementFragment;
import com.muzima.view.fragments.FillFormsFragment;
import com.muzima.view.fragments.observations.ObservationsListingFragment;
import org.jetbrains.annotations.NotNull;

public class DataCollectionViewPagerAdapter extends FragmentPagerAdapter {
    private final String patientUuid;

    public DataCollectionViewPagerAdapter(FragmentManager fm, String patientUuid) {
        super(fm, BEHAVIOR_SET_USER_VISIBLE_HINT);
        this.patientUuid = patientUuid;
    }

    @NotNull
    @Override
    public Fragment getItem(int position) {
        if (position == 0)
            return new FillFormsFragment(patientUuid);

        return new ObservationsListingFragment(Constants.OBSERVATIONS_FILTER_CATEGORY.BY_TYPE, patientUuid, true);
    }

    @Override
    public int getCount() {
        return 2;
    }
}
