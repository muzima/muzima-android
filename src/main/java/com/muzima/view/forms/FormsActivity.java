/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.muzima.service.TagPreferenceService;
import com.muzima.utils.Fonts;
import com.muzima.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.muzima.utils.Constants.DataSyncServiceConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;


public class FormsActivity extends FormsActivityBase {
    private static final String TAG = "FormsActivity";

    private ListView tagsDrawerList;
    private TextView tagsNoDataMsg;
    private DrawerLayout mainLayout;
    private ActionBarDrawerToggle actionbarDrawerToggle;
    private TagsListAdapter tagsListAdapter;
    private MenuItem menubarLoadButton;
    private FormController formController;
    private MenuItem tagsButton;
    private boolean syncInProgress;
    private MenuItem menuUpload;
    private TagPreferenceService tagPreferenceService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mainLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_forms, null);
        setContentView(mainLayout);
        super.onCreate(savedInstanceState);
        formController = ((MuzimaApplication) getApplication()).getFormController();
        tagPreferenceService = new TagPreferenceService(this);
        initDrawer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        tagsListAdapter.reloadData();
    }

    @Override
    protected void onDestroy() {
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
        menuUpload = menu.findItem(R.id.menu_upload);
        onPageChange(formsPager.getCurrentItem());
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
        tagPreferenceService.saveSelectedTags(newSelectedTags);
    }

    @Override
    protected void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        int syncStatus = intent.getIntExtra(DataSyncServiceConstants.SYNC_STATUS, SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(DataSyncServiceConstants.SYNC_TYPE, -1);

        if (syncType == DataSyncServiceConstants.SYNC_FORMS) {
            hideProgressbar();
            syncInProgress = false;
            if (syncStatus == SyncStatusConstants.SUCCESS) {
                tagsListAdapter.reloadData();
                ((FormsPagerAdapter) formsPagerAdapter).onFormMetadataDownloadFinish();
            }
        } else if (syncType == DataSyncServiceConstants.SYNC_UPLOAD_FORMS) {
            menuUpload.setActionView(null);
            syncInProgress = false;
            if (syncStatus == SyncStatusConstants.SUCCESS) {
                ((FormsPagerAdapter) formsPagerAdapter).onFormUploadFinish();
            }

        } else if (syncType == DataSyncServiceConstants.SYNC_TEMPLATES) {
            hideProgressbar();
            if (syncStatus == SyncStatusConstants.SUCCESS) {
                ((FormsPagerAdapter) formsPagerAdapter).onFormTemplateDownloadFinish();
            }
        } else if (syncType == DataSyncServiceConstants.SYNC_REAL_TIME_UPLOAD_FORMS) {
            SharedPreferences sp = getSharedPreferences("COMPLETED_FORM_AREA_IN_FOREGROUND", MODE_PRIVATE);
            if (sp.getBoolean("active", false) == true) {
                if (syncStatus == SyncStatusConstants.SUCCESS) {
                    ((FormsPagerAdapter) formsPagerAdapter).onFormUploadFinish();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.menu_load:
                if (!NetworkUtils.isConnectedToNetwork(this)) {
                    Toast.makeText(this, R.string.no_connection_found_msg, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (syncInProgress) {
                    Toast.makeText(this, R.string.already_fetching_forms_msg, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (hasFormsWithData()) {
                    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                    alertDialog.setMessage((getApplicationContext())
                                    .getString(R.string.already_exists_some_forms_with_patient_data_msg)
                    );
                    alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });
                    alertDialog.show();
                    return true;
                }
                syncAllFormsInBackgroundService();
                return true;
            case R.id.menu_upload:
                if (!NetworkUtils.isConnectedToNetwork(this)) {
                    Toast.makeText(this, R.string.no_connection_found_msg, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (syncInProgress) {
                    Toast.makeText(this, R.string.already_uploading_forms_msg, Toast.LENGTH_SHORT).show();
                    return true;
                }
                uploadAllFormsInBackgroundService();
                return true;
            case R.id.menu_client_add:
                intent = new Intent(this, RegistrationFormsActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_tags:
                if (mainLayout.isDrawerOpen(GravityCompat.END)) {
                    mainLayout.closeDrawer(GravityCompat.END);
                } else {
                    mainLayout.openDrawer(GravityCompat.END);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean hasFormsWithData() {
        try {
            if (!(formController.getAllIncompleteFormsWithPatientData().isEmpty() && formController.getAllCompleteFormsWithPatientData().isEmpty())) {
                return true;
            }
        } catch (FormController.FormFetchException e) {
        }
        return false;
    }

    private void syncAllFormsInBackgroundService() {
        syncInProgress = true;
        ((FormsPagerAdapter) formsPagerAdapter).onFormMetadataDownloadStart();
        showProgressBar();
        new SyncFormIntent(this).start();
    }

    private void uploadAllFormsInBackgroundService() {
        syncInProgress = true;
        menuUpload.setActionView(R.layout.refresh_menuitem);
        new UploadFormIntent(this).start();
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
        List<String> selectedTagsInPref = tagPreferenceService.getSelectedTags();
        List<Tag> allTags = null;
        try {
            allTags = formController.getAllTags();
        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Error occurred while get all tags from local repository", e);
        }
        List<Tag> selectedTags = new ArrayList<Tag>();

        if (selectedTagsInPref != null) {
            for (Tag tag : allTags) {
                if (selectedTagsInPref.contains(tag.getName())) {
                    selectedTags.add(tag);
                }
            }
        }
        formController.setSelectedTags(selectedTags);
    }

    @Override
    protected void onPageChange(int position) {
        ((FormsPagerAdapter) formsPagerAdapter).endActionMode();
        if (tagsButton == null || menuUpload == null || menubarLoadButton == null) {
            return;
        }
        switch (position) {
            case FormsPagerAdapter.TAB_All:
                showButtons(true, true, false);
                ((FormsPagerAdapter) formsPagerAdapter).unselectList();
                break;
            case FormsPagerAdapter.TAB_COMPLETE:
                showButtons(false, false, true);
                break;
            default:
                showButtons(false, false, false);
                break;
        }
    }

    private void showButtons(boolean tagButton, boolean menuLoadButton, boolean menuUploadButton) {
        tagsButton.setVisible(tagButton);
        menubarLoadButton.setVisible(menuLoadButton);
        menuUpload.setVisible(menuUploadButton);
    }
}
