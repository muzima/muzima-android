/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.custom;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.muzima.view.fragments.patient.DataCollectionFragment;
import com.muzima.view.fragments.patient.HistoricalDataFragment;

public class ClientSummaryPager extends FragmentStateAdapter {
    private final String patientUuid;
    private final Integer totalTabs;

    public ClientSummaryPager(@NonNull FragmentActivity fa, Integer totalTabs, String patientUuid) {
        super(fa);
        this.totalTabs = totalTabs;
        this.patientUuid = patientUuid;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1)
            return new DataCollectionFragment(patientUuid);

        return new HistoricalDataFragment(patientUuid);
    }

    @Override
    public int getItemCount() {
        return totalTabs;
    }
}
