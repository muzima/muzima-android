package com.muzima.view.cohort;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.forms.AllAvailableFormsAdapter;
import com.muzima.adapters.forms.TagsListAdapter;
import com.muzima.controller.FormController;
import com.muzima.service.MuzimaSyncService;
import com.muzima.utils.Fonts;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.HelpActivity;
import com.muzima.view.forms.MuzimaProgressDialog;

import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;


public class FormTemplateWizardActivity extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener {
    private MenuItem tagsButton;
    private DrawerLayout mainLayout;

    private ListView tagsDrawerList;
    private TextView tagsNoDataMsg;
    private ActionBarDrawerToggle actionbarDrawerToggle;
    private TagsListAdapter tagsListAdapter;
    private FormController formController;
    private AllAvailableFormsAdapter allAvailableFormsAdapter;
    private MuzimaProgressDialog progressDialog;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_form_templates_wizard, null);
        setContentView(mainLayout);
        formController = ((MuzimaApplication) getApplication()).getFormController();
        ListView listView = getListView();
        progressDialog = new MuzimaProgressDialog(this);
        allAvailableFormsAdapter = createAllFormsAdapter();
        allAvailableFormsAdapter.setBackgroundListQueryTaskListener(this);
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
            public void onClick(View view) {
                progressDialog.show("Downloading Form Templates...");

                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... voids) {
                        downloadFormTemplates();
                        navigateToNextActivity();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        progressDialog.dismiss();
                    }
                }.execute();
            }
        });

        Button previousButton = (Button) findViewById(R.id.previous);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToPreviousActivity();
            }
        });

        initDrawer();
    }

    private void navigateToPreviousActivity() {
        Intent intent = new Intent(getApplicationContext(), CohortWizardActivity.class);
        startActivity(intent);
        finish();
    }

    private void downloadFormTemplates() {
        List<String> selectedFormIdsArray = allAvailableFormsAdapter.getSelectedForms();
        MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
        int[] result = muzimaSyncService.downloadFormTemplates(selectedFormIdsArray.toArray(new String[selectedFormIdsArray.size()]));
        if (result[0] != SUCCESS) {
            Toast.makeText(this, "Could not download form templates", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(getApplicationContext(), CustomConceptWizardActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        tagsListAdapter.reloadData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.download_form_template_wizard, menu);
        MenuItem menuSettings = menu.findItem(R.id.action_settings);
        menuSettings.setEnabled(false);
        tagsButton = menu.findItem(R.id.menu_tags);
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
            case R.id.menu_tags:
                if (mainLayout.isDrawerOpen(GravityCompat.END)) {
                    mainLayout.closeDrawer(GravityCompat.END);
                } else {
                    mainLayout.openDrawer(GravityCompat.END);
                }
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

    private void initDrawer() {
        tagsDrawerList = (ListView) findViewById(R.id.tags_list);
        tagsDrawerList.setEmptyView(findViewById(R.id.tags_no_data_msg));
        tagsListAdapter = new TagsListAdapter(this, R.layout.item_tags_list, formController);
        tagsDrawerList.setAdapter(tagsListAdapter);
        tagsDrawerList.setOnItemClickListener(tagsListAdapter);
        tagsListAdapter.setTagsChangedListener(allAvailableFormsAdapter);
        actionbarDrawerToggle = new ActionBarDrawerToggle(this, mainLayout,
                R.drawable.ic_labels, R.string.drawer_open, R.string.drawer_close) {

            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                String title = getResources().getString(R.string.title_activity_form_list);
                getSupportActionBar().setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                String title = getResources().getString(R.string.drawer_title);
                getSupportActionBar().setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        };
        mainLayout.setDrawerListener(actionbarDrawerToggle);
        mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        tagsNoDataMsg = (TextView) findViewById(R.id.tags_no_data_msg);
        tagsNoDataMsg.setTypeface(Fonts.roboto_bold_condensed(this));
    }

    @Override
    public void onQueryTaskStarted() {
        progressDialog.show("Loading Forms...");
    }

    @Override
    public void onQueryTaskFinish() {
        progressDialog.dismiss();
    }
}

