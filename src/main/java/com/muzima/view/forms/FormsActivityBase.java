
package com.muzima.view.forms;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.utils.Fonts;
import com.muzima.view.customViews.PagerSlidingTabStrip;
import com.muzima.view.patients.MuzimaFragmentActivity;
import com.muzima.view.preferences.SettingsActivity;


public abstract class FormsActivityBase extends MuzimaFragmentActivity {
    private static final String TAG = "FormsActivityBase";

    public static int FORM_VIEW_ACTIVITY_RESULT = 1;


    protected ViewPager formsPager;
    protected PagerSlidingTabStrip pagerTabsLayout;
    protected MuzimaPagerAdapter formsPagerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPager();
        initPagerIndicator();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == FORM_VIEW_ACTIVITY_RESULT){
            formsPagerAdapter.reloadData();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void initPager() {
        formsPager = (ViewPager) findViewById(R.id.pager);
        formsPagerAdapter = createFormsPagerAdapter();
        formsPagerAdapter.initPagerViews();
        formsPager.setAdapter(formsPagerAdapter);
    }

    protected abstract MuzimaPagerAdapter createFormsPagerAdapter();


    private void initPagerIndicator() {
        pagerTabsLayout = (PagerSlidingTabStrip) findViewById(R.id.pager_indicator);
        pagerTabsLayout.setTextColor(Color.WHITE);
        pagerTabsLayout.setTextSize((int) getResources().getDimension(R.dimen.pager_indicator_text_size));
        pagerTabsLayout.setSelectedTextColor(getResources().getColor(R.color.tab_indicator));
        pagerTabsLayout.setTypeface(Fonts.roboto_medium(this), -1);
        pagerTabsLayout.setViewPager(formsPager);
        formsPager.setCurrentItem(0);
        pagerTabsLayout.markCurrentSelected(0);
        pagerTabsLayout.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                onPageChange(position);
            }
        });
    }

    protected void onPageChange(int position){
    }
}
