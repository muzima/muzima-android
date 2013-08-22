package com.muzima.view.cohort;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.cohort.CohortPagerAdapter;
import com.muzima.search.api.util.StringUtil;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.tasks.cohort.DownloadCohortTask;
import com.muzima.tasks.forms.DownloadFormMetadataTask;
import com.muzima.utils.Fonts;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.SettingsActivity;
import com.muzima.view.customViews.PagerSlidingTabStrip;

import static android.os.AsyncTask.Status.PENDING;
import static android.os.AsyncTask.Status.RUNNING;

public class CohortActivity extends SherlockFragmentActivity{
    private static final String TAG = "CohortActivity";
    private ViewPager viewPager;
    private CohortPagerAdapter cohortPagerAdapter;
    private PagerSlidingTabStrip pagerTabsLayout;
    private DownloadMuzimaTask cohortDownloadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cohort);
        initPager();
        initPagerIndicator();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.cohort_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.menu_load:
                if(!NetworkUtils.isConnectedToNetwork(this)){
                    Toast.makeText(this, "No connection found, please connect your device and try again", Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (cohortDownloadTask != null &&
                        (cohortDownloadTask.getStatus() == PENDING || cohortDownloadTask.getStatus() == RUNNING)) {
                    Toast.makeText(this, "Already fetching forms, ignored the request", Toast.LENGTH_SHORT).show();
                    return true;
                }
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                cohortDownloadTask = new DownloadCohortTask((MuzimaApplication) getApplicationContext());
                cohortDownloadTask.addDownloadListener(cohortPagerAdapter);
                String usernameKey = getResources().getString(R.string.preference_username);
                String passwordKey = getResources().getString(R.string.preference_password);
                String serverKey = getResources().getString(R.string.preference_server);
                String cohortPrefixKey = getResources().getString(R.string.preference_cohort_prefix);
                String[] credentials = new String[]{settings.getString(usernameKey, StringUtil.EMPTY),
                        settings.getString(passwordKey, StringUtil.EMPTY),
                        settings.getString(serverKey, StringUtil.EMPTY)};

                String prefix = settings.getString(cohortPrefixKey, StringUtil.EMPTY);
                if (StringUtil.EMPTY.equals(prefix)) {
                    cohortDownloadTask.execute(credentials);
                } else {
                    cohortDownloadTask.execute(credentials, new String[]{prefix});
                }

                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                overridePendingTransition(R.anim.push_in_from_left, R.anim.push_out_to_right);
                return true;
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
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
