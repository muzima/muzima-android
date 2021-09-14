/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.patients;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.muzima.utils.Constants;
import com.muzima.view.fragments.AddSingleElementFragment;
import com.muzima.view.fragments.FillFormsFragment;
import com.muzima.view.fragments.observations.ObservationsListingFragment;
import com.muzima.view.fragments.patient.HistoricalDataByEncounterFragment;
import com.muzima.view.fragments.patient.HistoricalDataByTypeFragment;

import org.jetbrains.annotations.NotNull;

public class DataCollectionViewPagerAdapter extends FragmentStateAdapter {
    private final String patientUuid;

    public DataCollectionViewPagerAdapter(@NonNull FragmentActivity fa, String patientUuid) {
        super(fa);
        this.patientUuid = patientUuid;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new FillFormsFragment(patientUuid);
            case 1:
                return new ObservationsListingFragment(Constants.OBSERVATIONS_FILTER_CATEGORY.BY_TYPE, patientUuid, true);
        }
        throw new AssertionError("This should never happen, let us blame the android operating system");
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
