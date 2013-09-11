
package com.muzima.view.forms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.FormsPagerAdapter;
import com.muzima.adapters.forms.TagsListAdapter;
import com.muzima.api.model.Tag;
import com.muzima.controller.FormController;
import com.muzima.search.api.util.StringUtil;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.tasks.forms.DownloadFormMetadataTask;
import com.muzima.utils.Fonts;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.RegisterClientActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.os.AsyncTask.Status.PENDING;
import static android.os.AsyncTask.Status.RUNNING;
import static com.muzima.utils.Constants.FORM_TAG_PREF;
import static com.muzima.utils.Constants.FORM_TAG_PREF_KEY;


public class FormsActivity extends FormsActivityBase {
    private static final String TAG = "FormsActivity";

    private DownloadMuzimaTask formDownloadTask;
    private ListView tagsDrawerList;
    private TextView tagsNoDataMsg;
    private DrawerLayout mainLayout;
    private ActionBarDrawerToggle actionbarDrawerToggle;
    private TagsListAdapter tagsListAdapter;
    private MenuItem menubarLoadButton;
    private FormController formController;
    private MenuItem tagsButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mainLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_forms, null);
        setContentView(mainLayout);
        super.onCreate(savedInstanceState);
        formController = ((MuzimaApplication) getApplication()).getFormController();
        initDrawer();
        setupActionbar();
    }

    private void setupActionbar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tagsListAdapter.reloadData();
    }

    @Override
    protected void onDestroy() {
        if (formDownloadTask != null) {
            formDownloadTask.cancel(false);
        }
        if (formController != null) {
            formController.resetTagColors();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.form_list_menu, menu);
        menubarLoadButton = menu.findItem(R.id.menu_load);
        tagsButton = menu.findItem(R.id.menu_tags);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        storeSelectedTags();

    }

    private void storeSelectedTags() {
        Set<String> newSelectedTags = new HashSet<String>();
        for (Tag selectedTag : formController.getSelectedTags()) {
            newSelectedTags.add(selectedTag.getName());
        }

        SharedPreferences cohortSharedPref = getSharedPreferences(FORM_TAG_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = cohortSharedPref.edit();
        editor.putStringSet(FORM_TAG_PREF_KEY, newSelectedTags);
        editor.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }

        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.menu_load:
                if (!NetworkUtils.isConnectedToNetwork(this)) {
                    Toast.makeText(this, "No connection found, please connect your device and try again", Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (formDownloadTask != null &&
                        (formDownloadTask.getStatus() == PENDING || formDownloadTask.getStatus() == RUNNING)) {
                    Toast.makeText(this, "Already fetching forms, ignored the request", Toast.LENGTH_SHORT).show();
                    return true;
                }
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                formDownloadTask = new DownloadFormMetadataTask((MuzimaApplication) getApplicationContext());
                formDownloadTask.addDownloadListener((FormsPagerAdapter) formsPagerAdapter);
                formDownloadTask.addDownloadListener(tagsListAdapter);
                String usernameKey = getResources().getString(R.string.preference_username);
                String passwordKey = getResources().getString(R.string.preference_password);
                String serverKey = getResources().getString(R.string.preference_server);
                String[] credentials = new String[]{settings.getString(usernameKey, StringUtil.EMPTY),
                        settings.getString(passwordKey, StringUtil.EMPTY),
                        settings.getString(serverKey, StringUtil.EMPTY)};
                menubarLoadButton.setActionView(R.layout.refresh_menuitem);
                formDownloadTask.execute(credentials);
                return true;
            case R.id.menu_client_add:
                intent = new Intent(this, RegisterClientActivity.class);
                startActivity(intent);
                return true;
            case android.R.id.home:
                finish();
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
    protected FormsPagerAdapter createFormsPagerAdapter() {
        return new FormsPagerAdapter(getApplicationContext(), getSupportFragmentManager());
    }

    public void hideProgressbar() {
        menubarLoadButton.setActionView(null);
    }

    public void showProgressBar() {
        menubarLoadButton.setActionView(R.layout.refresh_menuitem);
    }

    private void initDrawer() {
        initSelectedTags();
        tagsDrawerList = (ListView) findViewById(R.id.tags_list);
        tagsDrawerList.setEmptyView(findViewById(R.id.tags_no_data_msg));
        tagsListAdapter = new TagsListAdapter(this, R.layout.item_tags_list, formController);
        tagsDrawerList.setAdapter(tagsListAdapter);
        tagsDrawerList.setOnItemClickListener(tagsListAdapter);
        tagsListAdapter.setTagsChangedListener((FormsPagerAdapter) formsPagerAdapter);
        actionbarDrawerToggle = new ActionBarDrawerToggle(this, mainLayout,
                R.drawable.ic_labels, R.string.drawer_open, R.string.drawer_close) {

            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                String title = getResources().getString(R.string.title_activity_form_list);
                getSupportActionBar().setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
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

    private void initSelectedTags() {
        SharedPreferences cohortSharedPref = getSharedPreferences(FORM_TAG_PREF, MODE_PRIVATE);
        Set<String> selectedTagsInPref = cohortSharedPref.getStringSet(FORM_TAG_PREF_KEY, new HashSet<String>());
        List<Tag> allTags = null;
        try {
            allTags = formController.getAllTags();
        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Error occurred while get all tags from local repository\n" + e);
        }
        List<Tag> selectedTags = new ArrayList<Tag>();
        for (Tag tag : allTags) {
            if (selectedTagsInPref.contains(tag.getName())) {
                selectedTags.add(tag);
            }
        }
        formController.setSelectedTags(selectedTags);
    }

    @Override
    protected void onPageChange(int position) {
        if (tagsButton == null) {
            return;
        }

        if (position == FormsPagerAdapter.TAB_All) {
            tagsButton.setEnabled(true);
            tagsButton.setIcon(R.drawable.ic_labels);
        } else

        {
            tagsButton.setEnabled(false);
            tagsButton.setIcon(R.drawable.ic_labels_disabled);
        }
    }
}
