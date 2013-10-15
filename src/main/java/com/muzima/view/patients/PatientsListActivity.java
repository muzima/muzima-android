package com.muzima.view.patients;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.patients.PatientsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.utils.Fonts;
import com.muzima.view.RegisterClientActivity;
import com.muzima.view.preferences.SettingsActivity;

public class  PatientsListActivity extends SherlockActivity implements AdapterView.OnItemClickListener, ListAdapter.BackgroundListQueryTaskListener {
    public static final String COHORT_ID = "cohortId";
    public static final String COHORT_NAME = "cohortName";
    public static final String QUICK_SEARCH = "quickSearch";

    private ListView listView;
    private boolean quickSearch = false;
    private String cohortId = null;
    private PatientsAdapter cohortPatientsAdapter;
    private FrameLayout progressBarContainer;
    private View noDataView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_list);
        Bundle intentExtras = getIntent().getExtras();
        if(intentExtras != null){
            quickSearch = intentExtras.getBoolean(QUICK_SEARCH);
            cohortId = intentExtras.getString(COHORT_ID);
            String title = intentExtras.getString(COHORT_NAME);
            if(title!=null){
                setTitle(title);
            }
        }

        progressBarContainer = (FrameLayout) findViewById(R.id.progressbarContainer);

        setupActionbar();
        setupNoDataView();
        setupListView(cohortId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.client_list, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.search)
                .getActionView();
        searchView.setQueryHint("Search clients");
        setupNoSearchResultDataView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                cohortPatientsAdapter.search(s);
                return true;
            }
        });

        if (quickSearch) {
            searchView.setIconified(false);
            searchView.requestFocus();
        } else
            searchView.setIconified(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_client_add:
                Intent intent = new Intent(this, RegisterClientActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cohortPatientsAdapter.reloadData();
    }

    private void setupActionbar() {
        ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayShowTitleEnabled(true);
        supportActionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void setupListView(String cohortId) {
        listView = (ListView) findViewById(R.id.list);
        listView.setEmptyView(findViewById(R.id.no_data_layout));
        cohortPatientsAdapter = new PatientsAdapter(getApplicationContext(),
                R.layout.layout_list,
                ((MuzimaApplication) getApplicationContext()).getPatientController(),
                cohortId);
        cohortPatientsAdapter.setBackgroundListQueryTaskListener(this);
        listView.setAdapter(cohortPatientsAdapter);
        listView.setOnItemClickListener(this);
    }

    private void setupNoDataView() {
        noDataView = findViewById(R.id.no_data_layout);

        TextView noDataMsgTextView = (TextView) findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(getResources().getText(R.string.no_clients_downloaded));
        TextView noDataTipTextView = (TextView) findViewById(R.id.no_data_tip);
        noDataTipTextView.setText(R.string.no_clients_downloaded_tip);
        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(this));
        noDataTipTextView.setTypeface(Fonts.roboto_light(this));
    }

    private void setupNoSearchResultDataView() {
        TextView noDataMsgTextView = (TextView) findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(getResources().getText(R.string.no_clients_matched));
        TextView noDataTipTextView = (TextView) findViewById(R.id.no_data_tip);
        noDataTipTextView.setText(R.string.no_clients_matched_tip);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        Patient patient = cohortPatientsAdapter.getItem(position);
        Intent intent = new Intent(this, PatientSummaryActivity.class);
        intent.putExtra(PatientSummaryActivity.PATIENT,patient);
        startActivity(intent);
    }

    @Override
    public void onQueryTaskStarted() {
        listView.setVisibility(View.INVISIBLE);
        noDataView.setVisibility(View.INVISIBLE);
        progressBarContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onQueryTaskFinish() {
        listView.setVisibility(View.VISIBLE);
        progressBarContainer.setVisibility(View.INVISIBLE);
    }
}
