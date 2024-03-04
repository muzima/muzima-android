package com.muzima.view.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.patients.PatientsRemoteSearchAdapter;
import com.muzima.adapters.person.PersonSearchAdapter;
import com.muzima.api.model.HTCPerson;
import com.muzima.api.model.Location;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.Patient;
import com.muzima.api.model.User;
import com.muzima.controller.HTCPersonController;
import com.muzima.controller.LocationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.domain.Credentials;
import com.muzima.model.patient.PatientItem;
import com.muzima.scheduler.MuzimaJobScheduleBuilder;
import com.muzima.utils.Constants;
import com.muzima.utils.NetworkUtils;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.utils.VerticalSpaceItemDecoration;
import com.muzima.view.BaseActivity;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.htc.HTCFormActivity;
import com.muzima.view.person.PersonRegisterActivity;
import com.muzima.view.person.SearchSESPPersonActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.muzima.util.Constants.ServerSettings.DEFAULT_ENCOUNTER_LOCATION_SETTING;
import static com.muzima.utils.Constants.SEARCH_STRING_BUNDLE_KEY;

public class HTCMainActivity extends BaseActivity {
    private FloatingActionButton newPersonButton;
    private RecyclerView recyclerView;
    private PersonSearchAdapter personSearchAdapter;
    private List<PatientItem> searchResults;
    private EditText editTextSearch;
    private ImageButton searchButton;
    Toolbar toolbar;
    private HTCPersonController htcPersonController;

    private TextView userName;

    private User user;

    private static boolean isSyncRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_htcmain);

        this.user = ((MuzimaApplication) getApplication()).getAuthenticatedUser();
        this.userName = findViewById(R.id.user_name);
        this.userName.setText(getResources().getString(R.string.welcome_message)+this.user.getUsername());

        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.htc);


        this.searchResults = (List<PatientItem>) getIntent().getSerializableExtra("searchResults");
        this.searchResults = this.searchResults==null?new ArrayList<>():this.searchResults;
        searchButton = findViewById(R.id.buttonSearch);
        editTextSearch = findViewById(R.id.search);

        recyclerView = findViewById(R.id.person_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.addItemDecoration(new VerticalSpaceItemDecoration(16));
        personSearchAdapter = new PersonSearchAdapter(recyclerView, searchResults, this, getApplicationContext());
        recyclerView.setAdapter(personSearchAdapter);

        initController();
        getLatestHTCPersons();


        newPersonButton = findViewById(R.id.new_person);
        newPersonButton.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), PersonRegisterActivity.class);
            intent.putExtra("searchResults", (Serializable) searchResults);
            intent.putExtra("isEditionFlow", Boolean.FALSE);
            intent.putExtra("isAddATSForSESPExistingPerson", Boolean.FALSE);
            startActivity(intent);
        });

        searchButton.setOnClickListener(view -> {
            if(StringUtils.isEmpty(editTextSearch.getText().toString())) {
                AlertDialog.Builder builder = new AlertDialog.Builder(HTCMainActivity.this);
                builder.setCancelable(false)
                        .setIcon(ThemeUtils.getIconWarning(getApplicationContext()))
                        .setTitle(getResources().getString(R.string.general_success))
                        .setMessage(getResources().getString(R.string.fill_the_intended_name_to_search_on_sesp))
                        .setPositiveButton(R.string.general_ok, launchDashboard())
                        .show();
            } else {
                Intent intent = new Intent(getApplicationContext(), SearchSESPPersonActivity.class);
                intent.putExtra("searchValue", editTextSearch.getText().toString());
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_htc_main, menu);
        return true;
    }

    public boolean isDataSyncRunning(){
        return isSyncRunning;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sync_htc:
                processSync();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void processSync(){
        if(!isDataSyncRunning()) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.info_muzima_sync_service_in_progress), Toast.LENGTH_LONG).show();
            new MuzimaJobScheduleBuilder(getApplicationContext()).schedulePeriodicBackgroundJob(1000, true);

        }
    }

    private void initController() {
        this.htcPersonController = ((MuzimaApplication) getApplicationContext()).getHtcPersonController();
    }
    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        super.setSupportActionBar(toolbar);
    }

    private void getLatestHTCPersons() {
        List<HTCPerson> htcPersonList = this.htcPersonController.getLatestHTCPersons();
        searchResults.clear();
        for (HTCPerson htcPerson: htcPersonList) {
             PatientItem patientItem = new PatientItem(htcPerson);
             searchResults.add(patientItem);
        }
    }
    private DialogInterface.OnClickListener launchDashboard() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        };
    }
}
