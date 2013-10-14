package com.muzima.view.cohort;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.cohort.AllCohortsAdapter;
import com.muzima.service.DataSyncService;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.HelpActivity;
import com.muzima.view.MainActivity;

import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.*;


public class CohortWizardActivity extends BroadcastListenerActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cohort_wizard);
        ListView listView = getListView();
        final AllCohortsAdapter cohortsAdapter = createAllCohortsAdapter();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                cohortsAdapter.onListItemClick(position);
            }
        });

        cohortsAdapter.downloadCohortAndReload();
        listView.setAdapter(cohortsAdapter);

        Button nextButton = (Button) findViewById(R.id.next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadAndSavePatients(cohortsAdapter);
                markWizardHasEnded();
                navigateToNextActivity();
            }
        });

        Button previousButton = (Button) findViewById(R.id.previous);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToPreviousActivity();
            }
        });
    }

    private void navigateToPreviousActivity() {
        Intent intent = new Intent(getApplicationContext(), CohortPrefixWizardActivity.class);
        startActivity(intent);
    }

    private void downloadAndSavePatients(AllCohortsAdapter cohortsAdapter) {
        this.syncPatientsInBackgroundService(cohortsAdapter.getSelectedCohorts());
    }

    private void markWizardHasEnded() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String wizardFinishedKey = getResources().getString(R.string.preference_wizard_finished);
        settings.edit()
                .putBoolean(wizardFinishedKey, true)
                .commit();
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem menuSettings = menu.findItem(R.id.action_settings);
        menuSettings.setEnabled(false);
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

    private void syncPatientsInBackgroundService(List<String> selectedCohortsArray) {
        Intent intent = new Intent(this, DataSyncService.class);
        intent.putExtra(SYNC_TYPE, SYNC_PATIENTS_ONLY);
        intent.putExtra(CREDENTIALS, credentials().getCredentialsArray());
        intent.putExtra(COHORT_IDS, selectedCohortsArray.toArray(new String[selectedCohortsArray.size()]));
        startService(intent);
    }

}
