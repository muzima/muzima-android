/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.notifications;

import android.content.Intent;
import android.graphics.Color;
import android.support.v4.view.ViewPager;
import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.utils.Fonts;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.custom.PagerSlidingTabStrip;


public abstract class NotificationActivityBase extends BroadcastListenerActivity {

    private static final int NOTIFICATION_VIEW_ACTIVITY_RESULT = 1;

    private ViewPager notificationPager;
    MuzimaPagerAdapter notificationPagerAdapter;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == NOTIFICATION_VIEW_ACTIVITY_RESULT){
            notificationPagerAdapter.reloadData();
        }
    }

    void initPager() {
        notificationPager = findViewById(R.id.pager);
        notificationPagerAdapter = createNotificationsPagerAdapter();
        notificationPagerAdapter.initPagerViews();
        notificationPager.setAdapter(notificationPagerAdapter);
    }

    protected abstract MuzimaPagerAdapter createNotificationsPagerAdapter();


    void initPagerIndicator() {
        PagerSlidingTabStrip pagerTabsLayout = findViewById(R.id.pager_indicator);
        pagerTabsLayout.setTextColor(Color.WHITE);
        pagerTabsLayout.setTextSize((int) getResources().getDimension(R.dimen.pager_indicator_text_size));
        pagerTabsLayout.setSelectedTextColor(getResources().getColor(R.color.tab_indicator));
        pagerTabsLayout.setTypeface(Fonts.roboto_medium(this), -1);
        pagerTabsLayout.setViewPager(notificationPager);
        notificationPager.setCurrentItem(0);
        pagerTabsLayout.markCurrentSelected(0);
        pagerTabsLayout.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                onPageChange(position);
            }
        });
    }

    private void onPageChange(int position){
    }
}