/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.cohort;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.cohort.AllCohortsAdapter;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.SntpService;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.CheckedLinearLayout;
import com.muzima.view.HelpActivity;
import com.muzima.view.forms.FormTemplateWizardActivity;
import com.muzima.view.forms.MuzimaProgressDialog;

import java.io.IOException;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_COHORTS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;


public class CohortWizardActivity extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener {

    private MuzimaProgressDialog progressDialog;
    private boolean isProcessDialogOn = false;
    private final String TAG = "CohortWizardActivity" ;
    private PowerManager powerManager = null;
    private PowerManager.WakeLock wakeLock = null ;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cohort_wizard);
        ListView listView = getListView();
        final AllCohortsAdapter cohortsAdapter = createAllCohortsAdapter();

        final EditText filterCohortText = (EditText) findViewById(R.id.filter_cohorts_txt);
        filterCohortText.addTextChangedListener(textWatcherForFilterText(cohortsAdapter));

        ImageButton cancelFilterButton = (ImageButton) findViewById(R.id.cancel_filter_txt);
        cancelFilterButton.setOnClickListener(cancelFilterTextEventHandler(filterCohortText));
        Button nextButton = (Button) findViewById(R.id.next);
        nextButton.setOnClickListener(nextButtonClickListener(cohortsAdapter));

        progressDialog = new MuzimaProgressDialog(this);

        cohortsAdapter.setBackgroundListQueryTaskListener(this);
        listView.setOnItemClickListener(listViewClickListener(cohortsAdapter));

        cohortsAdapter.downloadCohortAndReload();
        listView.setAdapter(cohortsAdapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        removeSettingsMenu(menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isProcessDialogOn){
            turnOnProgressDialog("Loading Cohorts...");
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

    private View.OnClickListener cancelFilterTextEventHandler(final EditText filterCohortText) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                filterCohortText.setText("");
            }
        };
    }

    private AdapterView.OnItemClickListener listViewClickListener(final AllCohortsAdapter cohortsAdapter) {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckedLinearLayout checkedLinearLayout = (CheckedLinearLayout) view;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    checkedLinearLayout.toggle();
                }
                cohortsAdapter.onListItemClick(position);
            }
        };
    }

    private View.OnClickListener nextButtonClickListener(final AllCohortsAdapter cohortsAdapter) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOnProgressDialog("Downloading clients demographic...");
                new AsyncTask<Void, Void, int[]>() {

                    @Override
                    protected void onPreExecute() {
                        Log.i(TAG, "Canceling timer") ;
                        ((MuzimaApplication) getApplication()).cancelTimer();
                        keepPhoneAwake(true) ;
                    }

                    @Override
                    protected int[] doInBackground(Void... voids) {
                        return downloadAndSavePatients(cohortsAdapter);
                    }

                    @Override
                    protected void onPostExecute(int[] result) {
                        dismissProgressDialog();
                        if (result[0] != SUCCESS) {
                            Toast.makeText(CohortWizardActivity.this, "Could not download clients", Toast.LENGTH_SHORT).show();
                        }
                        Log.i(TAG, "Restarting timeout timer!") ;
                        ((MuzimaApplication) getApplication()).restartTimer();
                        try {
                            LastSyncTimeService lastSyncTimeService = ((MuzimaApplication)getApplicationContext()).getMuzimaContext().getLastSyncTimeService();
                            SntpService sntpService = ((MuzimaApplication)getApplicationContext()).getSntpService();
                            LastSyncTime lastSyncTime = new LastSyncTime(DOWNLOAD_COHORTS, sntpService.getLocalTime());
                            lastSyncTimeService.saveLastSyncTime(lastSyncTime);
                        } catch (IOException e) {
                            Log.i(TAG,"Error setting cohort sync time.");
                        }
                        keepPhoneAwake(false) ;
                        navigateToNextActivity();
                    }
                }.execute();
            }
        };
    }

    private void keepPhoneAwake(boolean awakeState) {
        Log.d(TAG, "Launching wake state: " + awakeState) ;
        if (awakeState) {
            powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
            wakeLock.acquire();
        } else {
            if(wakeLock != null) {
                wakeLock.release();
            }
        }
    }

    private TextWatcher textWatcherForFilterText(final AllCohortsAdapter cohortsAdapter) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                cohortsAdapter.filterItems(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
    }

    private int[] downloadAndSavePatients(AllCohortsAdapter cohortsAdapter) {
        MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();

        List<String> selectedCohortsArray = cohortsAdapter.getSelectedCohorts();
        return muzimaSyncService.downloadPatientsForCohorts(selectedCohortsArray.toArray(new String[selectedCohortsArray.size()]));
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(getApplicationContext(), FormTemplateWizardActivity.class);
        startActivity(intent);
        finish();
    }


    private AllCohortsAdapter createAllCohortsAdapter() {
        return new AllCohortsAdapter(getApplicationContext(), R.layout.item_cohorts_list, ((MuzimaApplication) getApplicationContext()).getCohortController());
    }

    private ListView getListView() {
        return (ListView) findViewById(R.id.cohort_wizard_list);
    }

    @Override
    public void onQueryTaskStarted() {
        turnOnProgressDialog("Loading Cohorts...");
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
