package com.muzima.view.patients;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.widget.SearchView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.patients.PatientsRemoteSearchAdapter;
import com.muzima.utils.Fonts;
import com.muzima.view.forms.RegistrationFormsActivity;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.muzima.utils.Constants.SEARCH_STRING_BUNDLE_KEY;

public class PatientRemoteSearchListActivity extends SherlockActivity implements AdapterView.OnItemClickListener,
        ListAdapter.BackgroundListQueryTaskListener {
    private PatientsRemoteSearchAdapter patientAdapter;
    private ListView listView;
    private String searchString;
    private FrameLayout progressBarContainer;

    private View noDataView;
    private ActionMode actionMode;

    private boolean actionModeActive = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_remote_search_list);
        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            searchString = intentExtras.getString(SEARCH_STRING_BUNDLE_KEY);
        }
        progressBarContainer = (FrameLayout) findViewById(R.id.progressbarContainer);

        setUpListView(searchString);
        setupNoDataView();
        patientAdapter.reloadData();
        Button createPatientBtn = (Button) findViewById(R.id.create_patient_btn);
        createPatientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PatientRemoteSearchListActivity.this, RegistrationFormsActivity.class));
            }
        });

    }

    private void setUpListView(String searchString) {
        listView = (ListView) findViewById(R.id.remote_search_list);
        listView.setEmptyView(findViewById(R.id.no_data_layout));
        patientAdapter = new PatientsRemoteSearchAdapter(getApplicationContext(),
                R.layout.activity_patient_remote_search_list,
                ((MuzimaApplication) getApplicationContext()).getPatientController(), searchString);
        patientAdapter.setBackgroundListQueryTaskListener(this);
        listView.setAdapter(patientAdapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.client_remote_list, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.search)
                .getActionView();
        searchView.setQueryHint("Search clients on server");
        return true;
    }

    @Override
    public void onQueryTaskStarted() {
        listView.setVisibility(INVISIBLE);
        noDataView.setVisibility(INVISIBLE);
        progressBarContainer.setVisibility(VISIBLE);
    }

    private void setupNoDataView() {

        noDataView = findViewById(R.id.no_data_layout);

        TextView noDataMsgTextView = (TextView) findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(getResources().getText(R.string.no_clients_matched_remotely));

        TextView noDataTipTextView = (TextView) findViewById(R.id.no_data_tip);
        noDataTipTextView.setText(R.string.no_clients_matched_tip_remotely);

        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(this));
        noDataTipTextView.setTypeface(Fonts.roboto_light(this));
    }

    @Override
    public void onQueryTaskFinish() {
        listView.setVisibility(VISIBLE);
        progressBarContainer.setVisibility(INVISIBLE);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (!actionModeActive && listView.getCheckedItemCount() > 0) {
            actionMode = this.startActionMode(new DownloadPatientMode());
            actionModeActive = true;
        } else if (listView.getCheckedItemCount() == 0) {
            actionMode.finish();
        }
    }

    private class DownloadPatientMode implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            PatientRemoteSearchListActivity.this.getSupportMenuInflater()
                    .inflate(R.menu.actionmode_menu_download, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, com.actionbarsherlock.view.MenuItem menuItem) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            for (int i = 0; i < listView.getChildCount(); i++)
                listView.setItemChecked(i, false);
        }
    }
}
