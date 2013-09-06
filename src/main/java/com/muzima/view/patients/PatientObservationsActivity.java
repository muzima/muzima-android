package com.muzima.view.patients;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.widget.SearchView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;
import com.muzima.adapters.observations.ObservationsByDateAdapter;
import com.muzima.adapters.observations.ObservationsPagerAdapter;
import com.muzima.utils.Fonts;
import com.muzima.view.customViews.PagerSlidingTabStrip;
import com.muzima.view.preferences.SettingsActivity;

public class PatientObservationsActivity extends SherlockFragmentActivity {

	public boolean quickSearch = false;
    private ViewPager viewPager;
    private ObservationsPagerAdapter cohortPagerAdapter;
    private PagerSlidingTabStrip pagerTabsLayout;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_patient_observations);
		
		Bundle extras = getIntent().getExtras();
        if (extras != null && "true".equals(extras.getString("quickSearch"))) {
            quickSearch = true;
        }
        // Show the Up button in the action bar.
		setupActionBar();
        initPager();
        initPagerIndicator();
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

    private void initPager() {
        viewPager = (ViewPager) findViewById(R.id.pager);
        cohortPagerAdapter = new ObservationsPagerAdapter(getApplicationContext(), getSupportFragmentManager());
        cohortPagerAdapter.initPagerViews();
        viewPager.setAdapter(cohortPagerAdapter);
    }

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.observation_list, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.search)
                .getActionView();
//        setupNoSearchResultDataView();
        searchView.setOnQueryTextListener(cohortPagerAdapter);
        return true;
    }

//    private void setupNoSearchResultDataView() {
//        setupNoDataView();
//        TextView noDataMsgTextView = (TextView) getActivity().findViewById(R.id.no_data_msg);
//        noDataMsgTextView.setText(getResources().getText(R.string.no_patients_matched));
//        TextView noDataTipTextView = (TextView) getActivity().findViewById(R.id.no_data_tip);
//        noDataTipTextView.setText(R.string.no_patients_matched_tip);
//    }
}
