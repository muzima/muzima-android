package com.muzima.view.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.person.PersonSearchAdapter;
import com.muzima.api.model.HTCPerson;
import com.muzima.api.model.User;

import com.muzima.controller.HTCPersonController;
import com.muzima.model.patient.PatientItem;
import com.muzima.scheduler.MuzimaJobScheduleBuilder;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.utils.VerticalSpaceItemDecoration;
import com.muzima.utils.ViewUtil;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.login.LoginActivity;
import com.muzima.view.person.PersonRegisterActivity;
import com.muzima.view.person.SearchSESPPersonActivity;
import com.muzima.view.preferences.SettingsActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HTCMainActivity extends BroadcastListenerActivity {
    private FloatingActionButton newPersonButton;
    private RecyclerView recyclerView;
    private PersonSearchAdapter personSearchAdapter;
    private List<PatientItem> searchResults;
    private EditText editTextSearch;
    private ImageButton searchButton;
    Toolbar toolbar;
    private HTCPersonController htcPersonController;

    private androidx.appcompat.app.AlertDialog syncDialog;
    private TextView userName;

    private User user;
    private Animation refreshIconRotateAnimation;



    private ActionMenuItemView syncMenuAction;


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
                        .setTitle(getResources().getString(R.string.general_error))
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

        syncMenuAction = findViewById(R.id.menu_sync_htc);
        return true;
    }

    private void logout() {
        if(((MuzimaApplication) getApplication()).getAuthenticatedUser() != null) {
            ((MuzimaApplication) getApplication()).logOut();
            launchLoginActivity();
        }
    }

    private void launchLoginActivity() {
        Intent intent = new Intent(getApplication(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(LoginActivity.isFirstLaunch, false);
        intent.putExtra(LoginActivity.sessionTimeOut, false);
        getApplication().startActivity(intent);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sync_htc:
                refreshIconRotateAnimation = AnimationUtils.loadAnimation(HTCMainActivity.this, R.anim.rotate_refresh);
                refreshIconRotateAnimation.setRepeatCount(Animation.INFINITE);
                processSync(refreshIconRotateAnimation);
                return true;
            case R.id.menu_log_out:
                showExitAlertDialog();
                return true;
            case R.id.menu_language:
                openLanguageDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openLanguageDialog() {
        Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
        intent.putExtra("MODULE", "ATS");
        startActivity(intent);
    }

    private void processSync(Animation refreshIconRotateAnimation){
        syncDialog = ViewUtil.displayAlertDialog(HTCMainActivity.this, "Envio de novos dados de ATS para o servidor em curso...");
        if(!isDataSyncRunning()) {
            syncMenuAction.startAnimation(refreshIconRotateAnimation);
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.info_muzima_sync_service_in_progress), Toast.LENGTH_LONG).show();
            new MuzimaJobScheduleBuilder(getApplicationContext()).schedulePeriodicBackgroundJob(1000, true);
            syncDialog.show();
            //showBackgroundSyncProgressDialog(HTCMainActivity.this);
        } else {
            syncDialog.show();
            //showBackgroundSyncProgressDialog(HTCMainActivity.this);
        }
    }

    protected void updateSyncProgressWidgets(boolean isSyncRunning){
        if(!isSyncRunning){
            syncMenuAction.clearAnimation();
            getLatestHTCPersons();
            personSearchAdapter.notifyDataSetChanged();
            if (syncDialog != null && syncDialog.isShowing()) syncDialog.dismiss();
            ViewUtil.displayAlertDialog(HTCMainActivity.this, "Envio de novos dados de ATS para o servidor efectuado com sucesso.").show();

        } else{
            syncMenuAction.startAnimation(refreshIconRotateAnimation);
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

    private void showExitAlertDialog() {
        String message = getResources().getString(R.string.warning_logout_confirm);

        new AlertDialog.Builder(HTCMainActivity.this)
                .setCancelable(true)
                .setIcon(ThemeUtils.getIconWarning(this))
                .setTitle(getResources().getString(R.string.title_logout_confirm))
                .setMessage(message)
                .setPositiveButton(getString(R.string.general_yes), exitApplication())
                .setNegativeButton(getString(R.string.general_no), null)
                .create()
                .show();
    }

    private Dialog.OnClickListener exitApplication() {
        return (dialog, which) -> {
            ((MuzimaApplication) getApplication()).logOut();
            launchLoginActivity();
        };
    }
}
