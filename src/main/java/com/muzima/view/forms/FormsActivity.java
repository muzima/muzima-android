
package com.muzima.view.forms;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.domain.Tag;
import com.muzima.tasks.DownloadFormTask;
import com.muzima.utils.CustomColor;
import com.muzima.utils.Fonts;
import com.muzima.view.RegisterClientActivity;
import com.muzima.view.customViews.PagerSlidingTabStrip;

import java.util.ArrayList;
import java.util.List;

import static android.os.AsyncTask.Status.PENDING;
import static android.os.AsyncTask.Status.RUNNING;
import static com.muzima.utils.Constants.FORMS_SERVER;
import static com.muzima.utils.Constants.PASS;
import static com.muzima.utils.Constants.USERNAME;


public class FormsActivity extends SherlockFragmentActivity {
    private static final String TAG = "FormsActivity";
    private DownloadFormTask formDownloadTask;
    private ViewPager formsPager;
    private PagerSlidingTabStrip pagerTabsLayout;
    private FormsPagerAdapter formsPagerAdapter;
    private ListView tagsDrawer;
    private DrawerLayout mainLayout;
    private ActionBarDrawerToggle actionbarDrawerToggle;

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
                formDownloadTask = new DownloadFormTask(((MuzimaApplication) getApplication()).getMuzimaContext());
                formDownloadTask.setDownloadListener(formsPagerAdapter);
                formDownloadTask.execute(USERNAME, PASS, FORMS_SERVER);
                return true;
            case R.id.client_add:
                Intent intent = new Intent(this, RegisterClientActivity.class);
                startActivity(intent);
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.tags:
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
        tagsDrawer = (ListView) findViewById(R.id.tags_drawer);
        tagsDrawer.setAdapter(new TagsListAdapter(this, R.layout.item_tags_list));
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

    private static class TagsListAdapter extends ArrayAdapter<Tag> {
        List<Tag> tags;

        public TagsListAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            tags = new ArrayList<Tag>();
            tags.add(new Tag("Patient", CustomColor.BLESSING.getColor()));
            tags.add(new Tag("Registration", CustomColor.ALLERGIC_RED.getColor()));
            tags.add(new Tag("PMTCT", CustomColor.POOLSIDE.getColor()));
            tags.add(new Tag("Observation", CustomColor.GRUBBY.getColor()));
            tags.add(new Tag("Natal Care", CustomColor.BLUSH.getColor()));

            addAll(tags);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater layoutInflater = LayoutInflater.from(getContext());
                convertView = layoutInflater.inflate(
                        R.layout.item_tags_list, parent, false);
                holder = new ViewHolder();
                holder.indicator = convertView.findViewById(R.id.tag_indicator);
                holder.name = (TextView) convertView
                        .findViewById(R.id.tag_name);
                holder.icon = (ImageView) convertView
                        .findViewById(R.id.tag_icon);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.indicator.setBackgroundColor(getItem(position).getColor());
            holder.name.setText(getItem(position).getName());
            holder.icon.setBackgroundColor(getItem(position).getColor());

            return convertView;
        }

        private static class ViewHolder {
            View indicator;
            TextView name;
            ImageView icon;
        }
    }

}
