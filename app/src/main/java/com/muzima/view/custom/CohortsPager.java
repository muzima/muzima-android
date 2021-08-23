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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import com.muzima.view.fragments.cohorts.AllCohortsListFragment;
import com.muzima.view.fragments.cohorts.AvailableCohortsFragment;
import com.muzima.view.fragments.cohorts.DownloadedCohortsFragment;

public class CohortsPager extends FragmentPagerAdapter {

    private final Integer totalTabs;

    public CohortsPager(@NonNull FragmentManager fm, Integer totalTabs) {
        super(fm, BEHAVIOR_SET_USER_VISIBLE_HINT);
        this.totalTabs = totalTabs;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position == 1)
            return new DownloadedCohortsFragment();
        else if (position == 2)
            return new AvailableCohortsFragment();

        return new AllCohortsListFragment();
    }

    @Override
    public int getCount() {
        return totalTabs;
    }
}
