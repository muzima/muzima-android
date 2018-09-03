/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.content.Intent;
import android.graphics.Color;
import android.support.v4.view.ViewPager;
import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.utils.Fonts;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.custom.PagerSlidingTabStrip;


public abstract class FormsActivityBase extends BroadcastListenerActivity {

    public static final int FORM_VIEW_ACTIVITY_RESULT = 1;
    public static final String KEY_FORMS_TAB_TO_OPEN = "formsTabToOpen";


    ViewPager formsPager;
    MuzimaPagerAdapter formsPagerAdapter;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == FORM_VIEW_ACTIVITY_RESULT){
            formsPagerAdapter.reloadData();
        }
    }

    void initPager() {
        formsPager = findViewById(R.id.pager);
        formsPagerAdapter = createFormsPagerAdapter();
        formsPagerAdapter.initPagerViews();
        formsPager.setAdapter(formsPagerAdapter);
    }

    protected abstract MuzimaPagerAdapter createFormsPagerAdapter();


    void initPagerIndicator() {
        Intent intent = getIntent();
        int tabToOpen = intent.getIntExtra(KEY_FORMS_TAB_TO_OPEN, -1);
        if (tabToOpen == -1) {
            tabToOpen = 0;
        }
        PagerSlidingTabStrip pagerTabsLayout = findViewById(R.id.pager_indicator);
        pagerTabsLayout.setTextColor(Color.WHITE);
        pagerTabsLayout.setTextSize((int) getResources().getDimension(R.dimen.pager_indicator_text_size));
        pagerTabsLayout.setSelectedTextColor(getResources().getColor(R.color.tab_indicator));
        pagerTabsLayout.setTypeface(Fonts.roboto_medium(this), -1);
        pagerTabsLayout.setViewPager(formsPager);
        formsPager.setCurrentItem(tabToOpen);
        pagerTabsLayout.markCurrentSelected(tabToOpen);
        pagerTabsLayout.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                onPageChange(position);
            }
        });
    }

    void onPageChange(int position){
    }
}
