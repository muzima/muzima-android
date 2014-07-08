package com.muzima.view.cohort;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.muzima.service.MuzimaSyncService;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.CheckedLinearLayout;
import com.muzima.view.HelpActivity;
import com.muzima.view.forms.MuzimaProgressDialog;

import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;


public class CohortWizardActivity extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener {

    private MuzimaProgressDialog progressDialog;
    private boolean isProcessDialogOn = false;

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
                    protected int[] doInBackground(Void... voids) {
                        return downloadAndSavePatients(cohortsAdapter);
                    }

                    @Override
                    protected void onPostExecute(int[] result) {
                        dismissProgressDialog();
                        if (result[0] != SyncStatusConstants.SUCCESS) {
                            Toast.makeText(CohortWizardActivity.this, "Could not download clients", Toast.LENGTH_SHORT).show();
                        }
                        navigateToNextActivity();
                    }
                }.execute();
            }
        };
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

    @Override
    public void onPause() {
        super.onPause();
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
