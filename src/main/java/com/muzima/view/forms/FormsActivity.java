
package com.muzima.view.forms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.FormsPagerAdapter;
import com.muzima.adapters.forms.TagsListAdapter;
import com.muzima.controller.FormController;
import com.muzima.search.api.util.StringUtil;
import com.muzima.tasks.forms.DownloadFormMetadataTask;
import com.muzima.tasks.forms.DownloadFormTask;
import com.muzima.utils.Fonts;
import com.muzima.view.RegisterClientActivity;
import com.muzima.view.customViews.PagerSlidingTabStrip;

import static android.os.AsyncTask.Status.PENDING;
import static android.os.AsyncTask.Status.RUNNING;


public class FormsActivity extends SherlockFragmentActivity{
    private static final String TAG = "FormsActivity";
    private DownloadFormTask formDownloadTask;
    private ViewPager formsPager;
    private PagerSlidingTabStrip pagerTabsLayout;
    private FormsPagerAdapter formsPagerAdapter;
    private ListView tagsDrawerList;
    private TextView tagsNoDataMsg;
    private DrawerLayout mainLayout;
    private ActionBarDrawerToggle actionbarDrawerToggle;
    private TagsListAdapter tagsListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_forms, null);
        setContentView(mainLayout);
        initPager();
        initPagerIndicator();
        initDrawer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        tagsListAdapter.reloadData();
    }

    @Override
    protected void onDestroy() {
        if (formDownloadTask != null) {
            formDownloadTask.cancel(true);
        }
        FormController formController = ((MuzimaApplication) getApplication()).getFormController();
        formController.resetTagColors();
        super.onDestroy();
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
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                formDownloadTask = new DownloadFormMetadataTask((MuzimaApplication) getApplicationContext());
                formDownloadTask.addDownloadListener(formsPagerAdapter);
                formDownloadTask.addDownloadListener(tagsListAdapter);
                String usernameKey = getResources().getString(R.string.preference_username);
                String passwordKey = getResources().getString(R.string.preference_password);
                String serverKey = getResources().getString(R.string.preference_server);
                String[] credentials = new String[]{settings.getString(usernameKey, StringUtil.EMPTY),
                        settings.getString(passwordKey, StringUtil.EMPTY),
                        settings.getString(serverKey, StringUtil.EMPTY)};
                formDownloadTask.execute(credentials);
                return true;
            case R.id.menu_client_add:
                Intent intent = new Intent(this, RegisterClientActivity.class);
                startActivity(intent);
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_tags:
                if (mainLayout.isDrawerOpen(GravityCompat.END)) {
                    mainLayout.closeDrawer(GravityCompat.END);
                } else {
                    mainLayout.openDrawer(GravityCompat.END);
                }
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

    private void initDrawer() {
        tagsDrawerList = (ListView) findViewById(R.id.tags_list);
        tagsDrawerList.setEmptyView(findViewById(R.id.tags_no_data_msg));
        tagsListAdapter = new TagsListAdapter(this, R.layout.item_tags_list, ((MuzimaApplication)getApplication()).getFormController());
        tagsDrawerList.setAdapter(tagsListAdapter);
        tagsDrawerList.setOnItemClickListener(tagsListAdapter);
        tagsListAdapter.setTagsChangedListener(formsPagerAdapter);
        actionbarDrawerToggle = new ActionBarDrawerToggle(this, mainLayout,
                R.drawable.ic_labels, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                String title = getResources().getString(R.string.title_activity_form_list);
                getSupportActionBar().setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                String title = getResources().getString(R.string.drawer_title);
                getSupportActionBar().setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        };
        mainLayout.setDrawerListener(actionbarDrawerToggle);
        mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        tagsNoDataMsg = (TextView) findViewById(R.id.tags_no_data_msg);
        tagsNoDataMsg.setTypeface(Fonts.roboto_bold_condensed(this));
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
        formsPager.setCurrentItem(0);
        pagerTabsLayout.markCurrentSelected(0);
    }
}
