package com.muzima.view.cohort;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;
import com.muzima.adapters.cohort.CohortPagerAdapter;
import com.muzima.utils.Fonts;
import com.muzima.view.customViews.PagerSlidingTabStrip;

public class CohortActivity extends SherlockFragmentActivity{
    private static final String TAG = "CohortActivity";
    private ViewPager viewPager;
    private CohortPagerAdapter cohortPagerAdapter;
    private PagerSlidingTabStrip pagerTabsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cohort);
        initPager();
        initPagerIndicator();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                overridePendingTransition(R.anim.push_in_from_left, R.anim.push_out_to_right);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_in_from_left, R.anim.push_out_to_right);
    }

    private void initPager() {
        viewPager = (ViewPager) findViewById(R.id.pager);
        cohortPagerAdapter = new CohortPagerAdapter(getApplicationContext(), getSupportFragmentManager());
        viewPager.setAdapter(cohortPagerAdapter);
    }

    private void initPagerIndicator() {
        pagerTabsLayout = (PagerSlidingTabStrip) findViewById(R.id.pager_indicator);
        pagerTabsLayout.setTextColor(Color.WHITE);
        pagerTabsLayout.setTextSize((int) getResources().getDimension(R.dimen.pager_indicator_text_size));
        pagerTabsLayout.setSelectedTextColor(getResources().getColor(R.color.tab_indicator));
        pagerTabsLayout.setTypeface(Fonts.roboto_medium(this), -1);
        pagerTabsLayout.setViewPager(viewPager);
        viewPager.setCurrentItem(0);
        pagerTabsLayout.markCurrentSelected(0);
    }
}
