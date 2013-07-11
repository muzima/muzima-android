
package com.muzima.view.forms;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.tasks.DownloadFormTask;
import com.muzima.tasks.DownloadTask;
import com.muzima.utils.Fonts;
import com.muzima.view.RegisterClientActivity;
import com.muzima.view.customViews.PagerSlidingTabStrip;

import static android.os.AsyncTask.Status.PENDING;
import static android.os.AsyncTask.Status.RUNNING;
import static com.muzima.utils.Constants.FORMS_SERVER;
import static com.muzima.utils.Constants.PASS;
import static com.muzima.utils.Constants.USERNAME;


public class FormsActivity extends SherlockFragmentActivity{
    private static final String TAG = "FormsActivity";
    private DownloadFormTask formDownloadTask;
    private ViewPager formsPager;
    private PagerSlidingTabStrip pagerTabsLayout;
    private FormsPagerAdapter formsPagerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forms);
        initPager();
        initPagerIndicator();
    }


    private void initPager() {
        formsPager = (ViewPager) findViewById(R.id.pager);
        formsPagerAdapter = new FormsPagerAdapter(getApplicationContext(), getSupportFragmentManager());
        formsPager.setAdapter(formsPagerAdapter);
    }

    private void initPagerIndicator() {
        pagerTabsLayout = (PagerSlidingTabStrip) findViewById(R.id.pager_indicator);
        pagerTabsLayout.setTextColor(Color.WHITE);
        pagerTabsLayout.setTextSize((int) getResources().getDimension(R.dimen.pager_indicator_text_size));
        pagerTabsLayout.setSelectedTextColor(getResources().getColor(R.color.tab_indicator));
        pagerTabsLayout.setTypeface(Fonts.roboto_medium(this), -1);
        pagerTabsLayout.setViewPager(formsPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.form_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_load:
                if (formDownloadTask != null &&
                        (formDownloadTask.getStatus() == PENDING || formDownloadTask.getStatus() == RUNNING)) {
                    Toast.makeText(this, "Already fetching forms, ignored the request", Toast.LENGTH_SHORT).show();
                    return true;
                }
                formDownloadTask = new DownloadFormTask(((MuzimaApplication)getApplication()).getMuzimaContext());
                formDownloadTask.setDownloadListener(formsPagerAdapter);
                formDownloadTask.execute(USERNAME, PASS, FORMS_SERVER);
                return true;
            case R.id.client_add:
                Intent intent = new Intent(this, RegisterClientActivity.class);
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }
}
