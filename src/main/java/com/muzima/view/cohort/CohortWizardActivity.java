package com.muzima.view.cohort;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.cohort.AllCohortsAdapter;
import com.muzima.service.MuzimaSyncService;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.HelpActivity;
import com.muzima.view.forms.MuzimaProgressDialog;

import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;


public class CohortWizardActivity extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener{

    private MuzimaProgressDialog progressDialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cohort_wizard);
        ListView listView = getListView();

        final AllCohortsAdapter cohortsAdapter = createAllCohortsAdapter();
        Button nextButton = (Button) findViewById(R.id.next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show("Downloading clients...");
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        downloadAndSavePatients(cohortsAdapter);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        progressDialog.dismiss();
                        navigateToNextActivity();
                    }
                }.execute();
            }
        });

        Button previousButton = (Button) findViewById(R.id.previous);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToPreviousActivity();
            }
        });

        progressDialog = new MuzimaProgressDialog(this);

        cohortsAdapter.setBackgroundListQueryTaskListener(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                cohortsAdapter.onListItemClick(position);
            }
        });

        cohortsAdapter.downloadCohortAndReload();
        listView.setAdapter(cohortsAdapter);
    }

    private void navigateToPreviousActivity() {
        Intent intent = new Intent(getApplicationContext(), CohortPrefixWizardActivity.class);
        startActivity(intent);
        finish();
    }

    private void downloadAndSavePatients(AllCohortsAdapter cohortsAdapter) {
        MuzimaSyncService muzimaSyncService = ((MuzimaApplication)getApplicationContext()).getMuzimaSyncService();
        List<String> selectedCohortsArray = cohortsAdapter.getSelectedCohorts();
        int[] result = muzimaSyncService.downloadPatientsForCohorts(selectedCohortsArray.toArray(new String[selectedCohortsArray.size()]));
        if (result[0] != SUCCESS) {
            Toast.makeText(this, "Could not download patients", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(getApplicationContext(), FormTemplateWizardActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        disableSettingsMenu(menu);
        return true;
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

    private AllCohortsAdapter createAllCohortsAdapter() {
        return new AllCohortsAdapter(getApplicationContext(), R.layout.item_cohorts_list, ((MuzimaApplication) getApplicationContext()).getCohortController());
    }

    private ListView getListView() {
        return (ListView) findViewById(R.id.cohort_wizard_list);
    }

    @Override
    public void onQueryTaskStarted() {
        progressDialog.show("Loading Cohorts...");
    }

    @Override
    public void onQueryTaskFinish() {
        progressDialog.dismiss();
    }
}
