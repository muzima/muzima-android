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
import com.muzima.view.fragments.forms.AllFormsListFragment;
import com.muzima.view.fragments.forms.AvailableFormsFragment;
import com.muzima.view.fragments.forms.DownloadedFormsFragment;

public class FormsPager extends FragmentPagerAdapter {

    private final Integer totalTabs;

    public FormsPager(@NonNull FragmentManager fm, Integer totalTabs) {
        super(fm, BEHAVIOR_SET_USER_VISIBLE_HINT);
        this.totalTabs = totalTabs;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position == 1)
            return new DownloadedFormsFragment();
        else if (position == 2)
            return new AvailableFormsFragment();

        return new AllFormsListFragment();
    }

    @Override
    public int getCount() {
        return totalTabs;
    }
}
