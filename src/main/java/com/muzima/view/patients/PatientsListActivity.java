package com.muzima.view.patients;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.patients.PatientsLocalSearchAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.User;
import com.muzima.search.api.util.StringUtil;
import com.muzima.utils.Fonts;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.forms.RegistrationFormsActivity;
import com.muzima.view.notifications.SyncNotificationsIntent;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.muzima.utils.Constants.DataSyncServiceConstants.*;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR;
import static com.muzima.utils.Constants.SEARCH_STRING_BUNDLE_KEY;

public class PatientsListActivity extends BroadcastListenerActivity implements AdapterView.OnItemClickListener, ListAdapter.BackgroundListQueryTaskListener {
    public static final String COHORT_ID = "cohortId";
    public static final String COHORT_NAME = "cohortName";
    public static final String QUICK_SEARCH = "quickSearch";
    public static final String NOTIFICATIONS = "Notifications";
    public static boolean isNotificationsList = false;
    private MenuItem menubarSyncButton;
    private boolean notificationsSyncInProgress;

    private ListView listView;
    private boolean quickSearch = false;
    private String cohortId = null;

    private PatientsLocalSearchAdapter patientAdapter;
    private FrameLayout progressBarContainer;
    private View noDataView;
    private String searchString;
    private Button searchServerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_list);
        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            quickSearch = intentExtras.getBoolean(QUICK_SEARCH);
            cohortId = intentExtras.getString(COHORT_ID);
            String title = intentExtras.getString(COHORT_NAME);
            if (title != null) {
                isNotificationsList = StringUtil.equals(title, NOTIFICATIONS);
                setTitle(title);
            }
        }  else
            isNotificationsList = false;

        progressBarContainer = (FrameLayout) findViewById(R.id.progressbarContainer);
        setupNoDataView();
        setupListView(cohortId);

        searchServerBtn = (Button) findViewById(R.id.search_server_btn);
        searchServerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PatientsListActivity.this, PatientRemoteSearchListActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(SEARCH_STRING_BUNDLE_KEY, searchString);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.client_list, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.search)
                .getActionView();
        searchView.setQueryHint("Search clients");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                searchString = s;
                activateRemoteAfterThreeCharacterEntered(s);
                patientAdapter.search(s.trim());
                return true;
            }
        });

        if (quickSearch) {
            searchView.setIconified(false);
            searchView.requestFocus();
        } else
            searchView.setIconified(true);

        menubarSyncButton = menu.findItem(R.id.menu_load);
        if (isNotificationsList)
            menubarSyncButton.setVisible(true);
        else
            menubarSyncButton.setVisible(false);

        super.onCreateOptionsMenu(menu);
        return true;
    }

    private void activateRemoteAfterThreeCharacterEntered(String searchString) {
        if(searchString.trim().length()<3){
            searchServerBtn.setVisibility(View.GONE);
        } else {
            searchServerBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_client_add:
                startActivity(new Intent(this, RegistrationFormsActivity.class));
                return true;
            case R.id.menu_load:
                if (notificationsSyncInProgress) {
                    Toast.makeText(this, "Action not allowed while sync is in progress", Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (!NetworkUtils.isConnectedToNetwork(this)) {
                    Toast.makeText(this, "No connection found, please connect your device and try again", Toast.LENGTH_SHORT).show();
                    return true;
                }

                syncAllNotificationsInBackgroundService();
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
        patientAdapter.reloadData();
    }

    private void setupListView(String cohortId) {
        listView = (ListView) findViewById(R.id.list);
        listView.setEmptyView(findViewById(R.id.no_data_layout));
        patientAdapter = new PatientsLocalSearchAdapter(getApplicationContext(),
                R.layout.layout_list,
                ((MuzimaApplication) getApplicationContext()).getPatientController(),
                cohortId, isNotificationsList);
        patientAdapter.setBackgroundListQueryTaskListener(this);
        listView.setAdapter(patientAdapter);
        listView.setOnItemClickListener(this);
    }

    private void setupNoDataView() {

        noDataView = findViewById(R.id.no_data_layout);

        TextView noDataMsgTextView = (TextView) findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(getResources().getText(R.string.no_clients_matched_locally));
        TextView noDataTipTextView = (TextView) findViewById(R.id.no_data_tip);
        noDataTipTextView.setText(R.string.no_clients_matched_tip_locally);
        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(this));
        noDataTipTextView.setTypeface(Fonts.roboto_light(this));
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        Patient patient = patientAdapter.getItem(position);
        Intent intent = new Intent(this, PatientSummaryActivity.class);
        intent.putExtra(PatientSummaryActivity.PATIENT, patient);
        startActivity(intent);
    }

    @Override
    public void onQueryTaskStarted() {
        listView.setVisibility(INVISIBLE);
        noDataView.setVisibility(INVISIBLE);
        progressBarContainer.setVisibility(VISIBLE);
    }

    @Override
    public void onQueryTaskFinish() {

        listView.setVisibility(VISIBLE);
        progressBarContainer.setVisibility(INVISIBLE);
    }

    @Override
    protected void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        int syncStatus = intent.getIntExtra(SYNC_STATUS, UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(SYNC_TYPE, -1);

        if (syncType == SYNC_NOTIFICATIONS) {
            hideProgressbar();
            onNotificationDownloadFinish();
        }
    }


    public void hideProgressbar() {
        menubarSyncButton.setActionView(null);
    }

    public void showProgressBar() {
        menubarSyncButton.setActionView(R.layout.refresh_menuitem);
    }

    private void syncAllNotificationsInBackgroundService() {
        notificationsSyncInProgress = true;
        onNotificationDownloadStart();
        showProgressBar();
        User authenticatedUser = ((MuzimaApplication) getApplicationContext()).getAuthenticatedUser();
        if (authenticatedUser != null)
            new SyncNotificationsIntent(this, authenticatedUser.getUuid()).start();
        else
            new SyncNotificationsIntent(this, null).start();
    }

    public void onNotificationDownloadFinish() {
        notificationsSyncInProgress = false;
        patientAdapter.reloadData();
        //updateSyncText();
    }

    public void onNotificationDownloadStart() {
        notificationsSyncInProgress = true;
    }
}
