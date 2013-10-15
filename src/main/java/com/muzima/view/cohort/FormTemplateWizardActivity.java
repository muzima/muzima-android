package com.muzima.view.cohort;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.AllAvailableFormsAdapter;
import com.muzima.service.DataSyncService;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.HelpActivity;
import com.muzima.view.MainActivity;

import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.CREDENTIALS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.FORM_IDS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TEMPLATES;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;


public class FormTemplateWizardActivity extends BroadcastListenerActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_templates_wizard);
        ListView listView = getListView();
        final AllAvailableFormsAdapter allAvailableFormsAdapter = createAllFormsAdapter();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                allAvailableFormsAdapter.onListItemClick(position);
            }
        });

        allAvailableFormsAdapter.downloadFormTemplatesAndReload();
        listView.setAdapter(allAvailableFormsAdapter);

        Button nextButton = (Button) findViewById(R.id.next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadFormTemplates();
                markWizardHasEnded();
                navigateToNextActivity();
            }
        });

        Button previousButton = (Button) findViewById(R.id.previous);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToPreviousActivity();
            }
        });
    }

    private void navigateToPreviousActivity() {
        Intent intent = new Intent(getApplicationContext(), CohortWizardActivity.class);
        startActivity(intent);
    }

    private void downloadFormTemplates() {
    }

    private void markWizardHasEnded() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String wizardFinishedKey = getResources().getString(R.string.preference_wizard_finished);
        settings.edit()
                .putBoolean(wizardFinishedKey, true)
                .commit();
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem menuSettings = menu.findItem(R.id.action_settings);
        menuSettings.setEnabled(false);
        return true;
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

    private AllAvailableFormsAdapter createAllFormsAdapter() {
        return new AllAvailableFormsAdapter(getApplicationContext(), R.layout.item_forms_list, ((MuzimaApplication) getApplicationContext()).getFormController());
    }

    private ListView getListView() {
        return (ListView) findViewById(R.id.form_template_wizard_list);
    }

    private void syncPatientsInBackgroundService(List<String> selectedFormIdsArray) {
        Intent intent = new Intent(this, DataSyncService.class);
        intent.putExtra(SYNC_TYPE, SYNC_TEMPLATES);
        intent.putExtra(CREDENTIALS, credentials().getCredentialsArray());
        intent.putExtra(FORM_IDS, selectedFormIdsArray.toArray(new String[selectedFormIdsArray.size()]));
        startService(intent);
    }

}
