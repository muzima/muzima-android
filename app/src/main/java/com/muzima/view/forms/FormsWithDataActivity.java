/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.legacy.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.util.Log;
import android.view.ActionMode;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.FormsPagerAdapter;
import com.muzima.adapters.forms.TagsListAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Tag;
import com.muzima.controller.FormController;
import com.muzima.controller.PatientController;
import com.muzima.service.TagPreferenceService;
import com.muzima.utils.DateUtils;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.NetworkUtils;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.MainDashboardActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.muzima.utils.Constants.DataSyncServiceConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import static com.muzima.view.patients.PatientSummaryActivity.PATIENT;
import static com.muzima.view.patients.PatientSummaryActivity.PATIENT_UUID;


public class FormsWithDataActivity extends FormsActivityBase {

    private DrawerLayout mainLayout;
    private TagsListAdapter tagsListAdapter;
    private Toolbar toolbar;
    private MenuItem menubarLoadButton;
    private MenuItem menuUpload;
    private MenuItem tagsButton;
    private FormController formController;
    private boolean syncInProgress;
    private TagPreferenceService tagPreferenceService;
    private String patientUuid;
    private final LanguageUtil languageUtil = new LanguageUtil();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        mainLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_forms, null);
        setContentView(mainLayout);
        formController = ((MuzimaApplication) getApplication()).getFormController();
        tagPreferenceService = new TagPreferenceService(this);

        patientUuid = getIntent().getStringExtra(PATIENT_UUID);

        loadPatientData();
        initPager();
        initDrawer();
        initPagerIndicator();
        logEvent("VIEW_FORM_DATA_LIST");
    }

    private void loadPatientData() {
        Patient patient = null;
        if(!StringUtils.isEmpty(patientUuid)) {
            try {
                patient = ((MuzimaApplication)getApplicationContext()).getPatientController().getPatientByUuid(patientUuid);
            } catch (PatientController.PatientLoadException e) {
                Log.e(getClass().getSimpleName(),"Could not load patient",e);
            }
        }

        if(patient!= null) {
            findViewById(R.id.client_summary_view).setVisibility(View.VISIBLE);
            TextView patientNameTextView = findViewById(R.id.name);
            ImageView patientGenderImageView = findViewById(R.id.genderImg);
            TextView dobTextView = findViewById(R.id.dateOfBirth);
            TextView identifierTextView = findViewById(R.id.identifier);
            TextView ageTextView = findViewById(R.id.age_text_label);

            patientNameTextView.setText(patient.getDisplayName());
            identifierTextView.setText(String.format(Locale.getDefault(), "ID:#%s", patient.getIdentifier()));
            dobTextView.setText(String.format("DOB: %s", new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(patient.getBirthdate())));
            patientGenderImageView.setImageResource(getGenderImage(patient.getGender()));
            ageTextView.setText(String.format(Locale.getDefault(), "%d Yrs", DateUtils.calculateAge(patient.getBirthdate())));
        }
    }

    private int getGenderImage(String gender) {
        return gender.equalsIgnoreCase("M") ? R.drawable.gender_male : R.drawable.gender_female;
    }

    @Override
    protected void onResume() {
        super.onResume();
        languageUtil.onResume(this);
        //tagsListAdapter.reloadData();
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
        getMenuInflater().inflate(R.menu.form_list_menu, menu);
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
        Set<String> newSelectedTags = new HashSet<>();
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

        switch (syncType) {
            case DataSyncServiceConstants.SYNC_FORMS:
                hideProgressbar();
                syncInProgress = false;
                if (syncStatus == SyncStatusConstants.SUCCESS) {
                    //tagsListAdapter.reloadData();
                    ((FormsPagerAdapter) formsPagerAdapter).onFormMetadataDownloadFinish();
                }
                break;
            case DataSyncServiceConstants.SYNC_UPLOAD_FORMS:
                menuUpload.setActionView(null);
                syncInProgress = false;
                if (syncStatus == SyncStatusConstants.SUCCESS) {
                    ((FormsPagerAdapter) formsPagerAdapter).onFormUploadFinish();
                    syncAllFormsInBackgroundService();
                }

                break;
            case DataSyncServiceConstants.SYNC_TEMPLATES:
                hideProgressbar();
                if (syncStatus == SyncStatusConstants.SUCCESS) {
                    ((FormsPagerAdapter) formsPagerAdapter).onFormTemplateDownloadFinish();
                }
                break;
            case DataSyncServiceConstants.SYNC_REAL_TIME_UPLOAD_FORMS:
                SharedPreferences sp = getSharedPreferences("COMPLETED_FORM_AREA_IN_FOREGROUND", MODE_PRIVATE);
                if (sp.getBoolean("active", false)) {
                    if (syncStatus == SyncStatusConstants.SUCCESS) {
                        ((FormsPagerAdapter) formsPagerAdapter).onFormUploadFinish();
                    }
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.menu_load:
                if (!NetworkUtils.isConnectedToNetwork(this)) {
                    Toast.makeText(this, R.string.error_local_connection_unavailable, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (syncInProgress) {
                    Toast.makeText(this, R.string.info_form_fetch_in_progress, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (hasFormsWithData()) {
                    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                    alertDialog.setMessage((getApplicationContext())
                                    .getString(R.string.error_patient_data_form_exist)
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
                    Toast.makeText(this, R.string.error_local_connection_unavailable, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (syncInProgress) {
                    Toast.makeText(this, R.string.info_form_upload_in_progress, Toast.LENGTH_SHORT).show();
                    return true;
                }
                uploadAllFormsInBackgroundService();
                return true;
            case R.id.menu_tags:
                if (mainLayout.isDrawerOpen(GravityCompat.END)) {
                    mainLayout.closeDrawer(GravityCompat.END);
                } else {
                    mainLayout.openDrawer(GravityCompat.END);
                }
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private boolean hasFormsWithData() {
        try {
            if (!(formController.getAllIncompleteFormsWithPatientData(patientUuid).isEmpty() &&
                    formController.getAllCompleteFormsWithPatientData(getApplicationContext(),patientUuid).isEmpty())) {
                return true;
            }
        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(),"Unable to fetch forms"+e.getMessage());
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

        logEvent("UPLOAD_FORM_DATA");
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
        ListView tagsDrawerList = findViewById(R.id.tags_list);
        tagsDrawerList.setEmptyView(findViewById(R.id.tags_no_data_msg));

        //ToDo: Reactivate when working on form tags
//        tagsListAdapter = new TagsListAdapter(this, R.layout.item_tags_list, formController);
//        tagsDrawerList.setAdapter(tagsListAdapter);
//        tagsDrawerList.setOnItemClickListener(tagsListAdapter);
//        tagsListAdapter.setTagsChangedListener((FormsPagerAdapter) formsPagerAdapter);
        ActionBarDrawerToggle actionbarDrawerToggle = new ActionBarDrawerToggle(this, mainLayout,
                R.drawable.ic_labels, R.string.hint_drawer_open, R.string.hint_drawer_close) {

            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                String title = getResources().getString(R.string.general_forms);
                getActionBar().setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                String title = getResources().getString(R.string.general_tags);
                getActionBar().setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        };
        mainLayout.setDrawerListener(actionbarDrawerToggle);
        mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    private void initSelectedTags() {
        List<String> selectedTagsInPref = tagPreferenceService.getSelectedTags();
        List<Tag> allTags = null;
        try {
            allTags = formController.getAllTags();
        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(), "Error occurred while get all tags from local repository", e);
        }
        List<Tag> selectedTags = new ArrayList<>();

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
       // ((FormsPagerAdapter) formsPagerAdapter).endActionMode();
        if (tagsButton == null || menuUpload == null || menubarLoadButton == null) {
            return;
        }
        switch (position) {
            case FormsPagerAdapter.TAB_INCOMPLETE:
                showButtons(false, false, false);
                //((FormsPagerAdapter) formsPagerAdapter).unselectList();
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

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
    }
}
