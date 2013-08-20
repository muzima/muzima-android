package com.muzima.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.listeners.DownloadListener;
import com.muzima.search.api.util.StringUtil;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.tasks.cohort.DownloadCohortTask;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.cohort.CohortActivity;
import com.muzima.view.forms.FormsActivity;
import com.muzima.view.patients.PatientsActivity;

import static android.os.AsyncTask.Status.PENDING;
import static android.os.AsyncTask.Status.RUNNING;

public class MainActivity extends SherlockActivity implements DownloadListener<Integer[]> {
    private static final String TAG = "MainActivity";
    private DownloadCohortTask downloadCohortTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
//		getOverflowMenu();

        ActionBar actionBar = getSupportActionBar();
        // actionBar.hide();
        actionBar.setDisplayShowTitleEnabled(true);
        // actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        // View cView
        // =getLayoutInflater().inflate(R.layout.actionbar_dashboard,null);
        // actionBar.setCustomView(cView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_help:
                intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_logout:
                intent = new Intent(this, LogoutActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        //overridePendingTransition(R.anim.fade_in, R.anim.push_out_to_left);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (downloadCohortTask != null) {
            downloadCohortTask.cancel(false);
        }
        super.onDestroy();
    }

    /**
     * Called when the user clicks the Cohort area
     */
    public void cohortList(View view) {
        Intent intent = new Intent(this, CohortActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.push_in_from_right, R.anim.push_out_to_left);
    }

    /**
     * Called when the user clicks the Clients area or Search Clients Button
     */
    public void patientList(View view) {
        Intent intent = new Intent(this, PatientsActivity.class);
        if (view.getId() == R.id.quickSearch) {
            intent.putExtra(PatientsActivity.QUICK_SEARCH, "true");
        }
        startActivity(intent);
        overridePendingTransition(R.anim.push_in_from_right, R.anim.push_out_to_left);
    }

    /**
     * Called when the user clicks the Forms area
     */
    public void formsList(View view) {
        Intent intent = new Intent(this, FormsActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.push_in_from_right, R.anim.push_out_to_left);
    }

    /**
     * Called when the user clicks the Notices area
     */
    public void noticesList(View view) {
        Intent intent = new Intent(this, NoticeListActivity.class);
        startActivity(intent);
    }

    /**
     * Called when the user clicks the Register Client Button
     */
    public void registerClient(View view) {
        Intent intent = new Intent(this, RegisterClientActivity.class);
        startActivity(intent);
    }

    /**
     * Called when the user clicks the Sync Button
     */
    public void sync(View view) {
        if (!NetworkUtils.isConnectedToNetwork(this)) {
            Toast.makeText(this, "No connection found, please connect your device and try again", Toast.LENGTH_SHORT).show();
            return;
        }

        if (downloadCohortTask != null &&
                (downloadCohortTask.getStatus() == PENDING || downloadCohortTask.getStatus() == RUNNING)) {
            Toast.makeText(this, "Already fetching forms, ignored the request", Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        downloadCohortTask = new DownloadCohortTask((MuzimaApplication) getApplicationContext());
        downloadCohortTask.addDownloadListener(this);
        String usernameKey = getResources().getString(R.string.preference_username);
        String passwordKey = getResources().getString(R.string.preference_password);
        String serverKey = getResources().getString(R.string.preference_server);
        String[] credentials = new String[]{settings.getString(usernameKey, StringUtil.EMPTY),
                settings.getString(passwordKey, StringUtil.EMPTY),
                settings.getString(serverKey, StringUtil.EMPTY)};
        downloadCohortTask.execute(credentials);
    }

    @Override
    public void downloadTaskComplete(Integer[] result) {
        Integer downloadStatus = result[0];
        String msg = "Download Complete with status " + downloadStatus;
        Log.i(TAG, msg);
        if (downloadStatus == DownloadMuzimaTask.SUCCESS) {
            msg = "Cohorts downloaded: " + result[1];
        } else if (downloadStatus == DownloadMuzimaTask.DOWNLOAD_ERROR) {
            msg = "An error occurred while downloading forms";
        } else if (downloadStatus == DownloadMuzimaTask.AUTHENTICATION_ERROR) {
            msg = "Authentication error occurred while downloading forms";
        } else if (downloadStatus == DownloadMuzimaTask.CONNECTION_ERROR) {
            msg = "Connection error occurred while downloading forms";
        } else if (downloadStatus == DownloadMuzimaTask.PARSING_ERROR) {
            msg = "Parse exception has been thrown while fetching data";
        }
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
