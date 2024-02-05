package com.muzima.view.main;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.patients.PatientsRemoteSearchAdapter;
import com.muzima.adapters.person.PersonSearchAdapter;
import com.muzima.api.model.HTCPerson;
import com.muzima.api.model.Patient;
import com.muzima.controller.HTCPersonController;
import com.muzima.domain.Credentials;
import com.muzima.model.patient.PatientItem;
import com.muzima.utils.Constants;
import com.muzima.utils.NetworkUtils;
import com.muzima.utils.VerticalSpaceItemDecoration;
import com.muzima.view.BaseActivity;
import com.muzima.view.person.PersonRegisterActivity;

import java.util.ArrayList;
import java.util.List;

public class HTCMainActivity extends BaseActivity {

    private FloatingActionButton newPersonButton;

    private RecyclerView recyclerView;
    private PersonSearchAdapter personSearchAdapter;

    private List<PatientItem> searchResults;

    private EditText editTextSearch;

    private ImageButton searchButton;


    private HTCPersonController htcPersonController;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_htcmain);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("ATS");

        this.searchResults = new ArrayList<>();
        searchButton = findViewById(R.id.buttonSearch);
        editTextSearch = findViewById(R.id.search);

        recyclerView = findViewById(R.id.person_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.addItemDecoration(new VerticalSpaceItemDecoration(16));
        personSearchAdapter = new PersonSearchAdapter(recyclerView, searchResults, this, getApplicationContext());
        recyclerView.setAdapter(personSearchAdapter);

        initController();

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        newPersonButton = findViewById(R.id.new_person);
        newPersonButton.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), PersonRegisterActivity.class);
            startActivity(intent);
        });

        searchButton.setOnClickListener(view -> {
            new ServerHTCPersonSearchBackgroundTask().execute(editTextSearch.getText().toString());
        });
    }

    private void initController() {
        this.htcPersonController = ((MuzimaApplication) getApplicationContext()).getHtcPersonController();
    }

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        super.setSupportActionBar(toolbar);
    }

    private class ServerHTCPersonSearchBackgroundTask extends AsyncTask<String, Void, Object> {

        @Override
        protected void onPreExecute() {
            onPreExecuteUpdate();
        }
        @Override
        protected void onPostExecute(Object patientsObject) {
            List<HTCPerson> htcPersonList = (List<HTCPerson>)patientsObject;
            onPostExecuteUpdate(htcPersonList);
        }
        @Override
        protected Object doInBackground(String... strings) {
            MuzimaApplication applicationContext = (MuzimaApplication) getApplicationContext();

            Credentials credentials = new Credentials(applicationContext);
            try {
                Constants.SERVER_CONNECTIVITY_STATUS serverStatus = NetworkUtils.getServerStatus(applicationContext, credentials.getServerUrl());
                if(serverStatus == Constants.SERVER_CONNECTIVITY_STATUS.SERVER_ONLINE) {
                    int authenticateResult = applicationContext.getMuzimaSyncService().authenticate(credentials.getCredentialsArray());
                    if (authenticateResult == Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_SUCCESS) {
                        return htcPersonController.searchPersonOnServer(strings[0]);
                    } else {
                        cancel(true);
                        return authenticateResult;
                    }
                }else {
                    cancel(true);
                    return serverStatus;
                }

            } catch (Throwable t) {
                Log.e(getClass().getSimpleName(), "Error while searching for person in the server.", t);
            } finally {
                applicationContext.getMuzimaContext().closeSession();
            }
            Log.e(getClass().getSimpleName(), "Authentication failure !! Returning empty patient list");
            return new ArrayList<Patient>();
        }
    }

    private void onPreExecuteUpdate() {
        this.searchResults.clear();
        this.personSearchAdapter.notifyDataSetChanged();
    }

    private void onPostExecuteUpdate(List<HTCPerson> htcPersonList) {
        if (htcPersonList == null) {
            Toast.makeText(this, getApplicationContext().getString(R.string.error_patient_repo_fetch), Toast.LENGTH_SHORT).show();
            return;
        }
        this.searchResults.clear();

        for(HTCPerson patient:htcPersonList) {
            this.searchResults.add(new PatientItem(patient));
        }

        this.personSearchAdapter.notifyDataSetChanged();
    }
}