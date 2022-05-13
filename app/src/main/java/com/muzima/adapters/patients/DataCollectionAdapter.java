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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.muzima.view.fragments.patient.AddSingleElementFragment;
import com.muzima.view.fragments.patient.PatientFillFormsFragment;

public class DataCollectionAdapter extends FragmentPagerAdapter {
    private final String patientUuid;
    private final Integer totalTabs;
    private final boolean isSingleElementEnabled;

    public DataCollectionAdapter(@NonNull FragmentManager fm, Integer totalTabs, String patientUuid, boolean isSingleElementEnabled) {
        super(fm, BEHAVIOR_SET_USER_VISIBLE_HINT);
        this.totalTabs = totalTabs;
        this.patientUuid = patientUuid;
        this.isSingleElementEnabled = isSingleElementEnabled;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position == 1)
            return new AddSingleElementFragment(patientUuid);
        else
            return new PatientFillFormsFragment(patientUuid);
    }

    @Override
    public int getCount() {
        return totalTabs;
    }
}
