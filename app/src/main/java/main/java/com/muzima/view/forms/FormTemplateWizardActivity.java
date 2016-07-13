/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.forms.AllAvailableFormsAdapter;
import com.muzima.adapters.forms.TagsListAdapter;
import com.muzima.api.model.APIName;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.SntpService;
import com.muzima.utils.Fonts;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.HelpActivity;
import com.muzima.view.cohort.CohortWizardActivity;
import com.muzima.view.location.CustomLocationWizardActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;


public class FormTemplateWizardActivity extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener {
    private MenuItem tagsButton;
    private DrawerLayout mainLayout;

    private ListView tagsDrawerList;
    private TextView tagsNoDataMsg;
    private ActionBarDrawerToggle actionbarDrawerToggle;
    private TagsListAdapter tagsListAdapter;
    private FormController formController;
    private AllAvailableFormsAdapter allAvailableFormsAdapter;
    private MuzimaProgressDialog progressDialog;
    private ListView listView;
    private boolean isProcessDialogOn = false;
    private static final String TAG = "FormTemplateWizardActivity";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_form_templates_wizard, null);
        setContentView(mainLayout);
        formController = ((MuzimaApplication) getApplication()).getFormController();
        listView = getListView();
        progressDialog = new MuzimaProgressDialog(this);
        allAvailableFormsAdapter = createAllFormsAdapter();
        allAvailableFormsAdapter.setBackgroundListQueryTaskListener(this);
        ImageButton tags = (ImageButton) findViewById(R.id.form_tags);
        tags.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mainLayout.isDrawerOpen(GravityCompat.END)) {
                    mainLayout.closeDrawer(GravityCompat.END);
                } else {
                    mainLayout.openDrawer(GravityCompat.END);
                }
            }
        });
        allAvailableFormsAdapter.downloadFormTemplatesAndReload();
        listView.setAdapter(allAvailableFormsAdapter);

        Button nextButton = (Button) findViewById(R.id.next);
        nextButton.setOnClickListener(nextButtonListener());

        Button previousButton = (Button) findViewById(R.id.previous);
        previousButton.setOnClickListener(previousButtonListener());

        initDrawer();
    }

    private View.OnClickListener previousButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToPreviousActivity();
            }
        };
    }

    private View.OnClickListener nextButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hasRegistrationFormSelected()) {
                    Toast.makeText(FormTemplateWizardActivity.this, "Please select at least one registration form!", Toast.LENGTH_SHORT).show();
                    return;
                }
                turnOnProgressDialog("Downloading Form Templates...");

                new AsyncTask<Void, Void, int[]>() {

                    @Override
                    protected int[] doInBackground(Void... voids) {
                        return downloadFormTemplates();
                    }

                    @Override
                    protected void onPostExecute(int[] result) {
                        dismissProgressDialog();
                        if (result[0] != SyncStatusConstants.SUCCESS) {
                            Toast.makeText(FormTemplateWizardActivity.this, "Could not download form templates", Toast.LENGTH_SHORT).show();
                        }
                        try {
                            LastSyncTimeService lastSyncTimeService = ((MuzimaApplication)getApplicationContext()).getMuzimaContext().getLastSyncTimeService();
                            SntpService sntpService = ((MuzimaApplication)getApplicationContext()).getSntpService();
                            LastSyncTime lastSyncTime = new LastSyncTime(APIName.DOWNLOAD_FORMS,sntpService.getLocalTime());
                            lastSyncTimeService.saveLastSyncTime(lastSyncTime);
                        } catch (IOException e) {
                            Log.i(TAG, "Error getting forms last sync time");
                        }
                        navigateToNextActivity();
                    }
                }.execute();
            }
        };
    }

    private boolean hasRegistrationFormSelected() {
        SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
        boolean registrationFormSelected = false;
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.valueAt(i)) {
                AvailableForm selectedForm = (AvailableForm) listView.getItemAtPosition(checkedItemPositions.keyAt(i));
                if (selectedForm.isRegistrationForm()) {
                    registrationFormSelected = true;
                }
            }
        }
        return registrationFormSelected;
    }

    private void navigateToPreviousActivity() {
        Intent intent = new Intent(getApplicationContext(), CohortWizardActivity.class);
        startActivity(intent);
        finish();
    }

    private int[] downloadFormTemplates() {
        List<String> selectedFormIdsArray = getSelectedForms();
        MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
        return muzimaSyncService.downloadFormTemplates(selectedFormIdsArray.toArray(new String[selectedFormIdsArray.size()]), false);
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(getApplicationContext(), CustomLocationWizardActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        tagsListAdapter.reloadData();
        if(isProcessDialogOn){
            turnOnProgressDialog("Downloading Form Templates...");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                Intent intent = new Intent(this, HelpActivity.class);
                intent.putExtra(HelpActivity.HELP_TYPE, HelpActivity.COHORT_WIZARD_HELP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private AllAvailableFormsAdapter createAllFormsAdapter() {
        return new AllAvailableFormsAdapter(getApplicationContext(), R.layout.item_forms_list_selectable, ((MuzimaApplication) getApplicationContext()).getFormController());
    }

    private ListView getListView() {
        return (ListView) findViewById(R.id.form_template_wizard_list);
    }

    private void initDrawer() {
        tagsDrawerList = (ListView) findViewById(R.id.tags_list);
        tagsDrawerList.setEmptyView(findViewById(R.id.tags_no_data_msg));
        tagsListAdapter = new TagsListAdapter(this, R.layout.item_tags_list, formController);
        tagsDrawerList.setAdapter(tagsListAdapter);
        tagsDrawerList.setOnItemClickListener(tagsListAdapter);
        tagsListAdapter.setTagsChangedListener(allAvailableFormsAdapter);
        actionbarDrawerToggle = new ActionBarDrawerToggle(this, mainLayout,
                R.drawable.ic_labels, R.string.drawer_open, R.string.drawer_close) {

            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        };
        mainLayout.setDrawerListener(actionbarDrawerToggle);
        mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        tagsNoDataMsg = (TextView) findViewById(R.id.tags_no_data_msg);
        tagsNoDataMsg.setTypeface(Fonts.roboto_bold_condensed(this));
    }

    private List<String> getSelectedForms() {
        List<String> formUUIDs = new ArrayList<String>();
        SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.valueAt(i)) {
                formUUIDs.add(((AvailableForm) listView.getItemAtPosition(checkedItemPositions.keyAt(i))).getFormUuid());
            }
        }
        return formUUIDs;
    }

    @Override
    public void onQueryTaskStarted() {
        turnOnProgressDialog("Loading Forms...");
    }

    @Override
    public void onQueryTaskFinish() {
        dismissProgressDialog();
    }

    private void turnOnProgressDialog(String message){
        progressDialog.show(message);
        isProcessDialogOn = true;
    }

    private void dismissProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
            isProcessDialogOn = false;
        }
    }
}

