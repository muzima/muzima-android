package com.muzima.view.patients;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.patients.PatientsAdapter;
import com.muzima.utils.Fonts;
import com.muzima.view.HelpActivity;
import com.muzima.view.LogoutActivity;
import com.muzima.view.RegisterClientActivity;
import com.muzima.view.SettingsActivity;

public class PatientsActivity extends SherlockActivity {
    public static final String COHORT_ID = "cohortId";
    public static final String QUICK_SEARCH = "quickSearch";

    private ListView listView;
    private boolean quickSearch = false;
    private String cohortId = null;
    private PatientsAdapter cohortPatientsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_list);
        Bundle intentExtras = getIntent().getExtras();
        if(intentExtras != null){
            quickSearch = intentExtras.getBoolean(QUICK_SEARCH);
            cohortId = intentExtras.getString(COHORT_ID);
        }

        setupActionbar();
        setupNoDataView();
        setupListView(cohortId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.client_list, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.search)
                .getActionView();

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
                NavUtils.navigateUpFromSameTask(this);
                overridePendingTransition(R.anim.push_in_from_left, R.anim.push_out_to_right);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_in_from_left, R.anim.push_out_to_right);
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
        listView.setAdapter(cohortPatientsAdapter);
    }

    private void setupNoDataView() {
        TextView noDataMsgTextView = (TextView) findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(getResources().getText(R.string.no_patients_downloaded));
        TextView noDataTipTextView = (TextView) findViewById(R.id.no_data_tip);
        noDataTipTextView.setText(R.string.no_patients_downloaded_tip);
        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(this));
        noDataTipTextView.setTypeface(Fonts.roboto_light(this));
    }
}
