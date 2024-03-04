package com.muzima.view.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
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
import com.muzima.api.model.Location;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.Patient;
import com.muzima.controller.HTCPersonController;
import com.muzima.controller.LocationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.domain.Credentials;
import com.muzima.model.patient.PatientItem;
import com.muzima.utils.Constants;
import com.muzima.utils.NetworkUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.utils.VerticalSpaceItemDecoration;
import com.muzima.view.BaseActivity;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_htcmain);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Registar ATS");

        this.searchResults = (List<PatientItem>) getIntent().getSerializableExtra("searchResults");
        this.searchResults = this.searchResults==null?new ArrayList<>():this.searchResults;
        searchButton = findViewById(R.id.buttonSearch);
        editTextSearch = findViewById(R.id.search);

        recyclerView = findViewById(R.id.person_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.addItemDecoration(new VerticalSpaceItemDecoration(16));
        personSearchAdapter = new PersonSearchAdapter(recyclerView, searchResults, this, getApplicationContext(), true);
        recyclerView.setAdapter(personSearchAdapter);

        initController();
        getLatestHTCPersons();

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationIcon(R.drawable.ic_refresh);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        newPersonButton = findViewById(R.id.new_person);
        newPersonButton.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), PersonRegisterActivity.class);
            intent.putExtra("searchResults", (Serializable) searchResults);
            intent.putExtra("isNewPerson", Boolean.TRUE);
            startActivity(intent);
        });

        searchButton.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), SearchSESPPersonActivity.class);
            intent.putExtra("searchValue", editTextSearch.getText().toString());
            startActivity(intent);
        });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.drawable.ic_refresh:
                        AlertDialog.Builder builder = new AlertDialog.Builder(HTCMainActivity.this);
                        builder.setCancelable(false)
                                .setIcon(ThemeUtils.getIconWarning(getApplicationContext()))
                                .setTitle(getResources().getString(R.string.general_success))
                                .setMessage(getResources().getString(R.string.record_saved_sucessfull))
                                .setPositiveButton(R.string.general_ok, null)
                                .show();
                        return true;
                    default:
                }
                return false;
            }
        });
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
}