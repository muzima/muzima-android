
package com.muzima.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.db.Html5FormDataSource;
import com.muzima.service.FormsService;
import com.muzima.utils.Fonts;


public class FormsActivity extends SherlockActivity implements ActionBar.TabListener, FormsService.OnDataFetchComplete {
    private static final String TAG = "FormsActivity";
    private ListView formsList;
    private View noDataView;
    private TextView noDataText;
    private TextView noDataTip;

    private FormsService formsService;
    private Html5FormDataSource html5FormDataSource;
    private Html5FormsAdapter html5FormsAdapter;

    private ActionBar.Tab completeTab;
    private ActionBar.Tab newTab;
    private ActionBar.Tab incompleteTab;
    private ActionBar.Tab syncedTab;

    private String noNewFormMsg;
    private String noNewFormTip;
    private String noIncompleteFormMsg;
    private String noIncompleteFormTip;
    private String noCompleteFormMsg;
    private String noCompleteFormTip;
    private String noSyncedFormMsg;
    private String noSyncedFormTip;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forms);

        formsService = ((MuzimaApplication) getApplication()).getFormsService();
        formsService.setDataFetchListener(this);

        html5FormDataSource = ((MuzimaApplication) getApplication()).getHtml5FormDataSource();
        html5FormDataSource.open();
        html5FormsAdapter = new Html5FormsAdapter(this, R.layout.form_list_item, html5FormDataSource);

        formsList = (ListView) findViewById(R.id.forms_list);
        noDataView = findViewById(R.id.no_data_layout);

        setupNoDataView();

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        initTabs();
        getSupportActionBar().selectTab(newTab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        html5FormDataSource.open();
    }

    @Override
    protected void onPause() {
        super.onPause();
        html5FormDataSource.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.form_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_load:
                formsService.fetchForms();
                return true;
            case R.id.client_add:
                Intent intent = new Intent(this, RegisterClientActivity.class);
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onFetch(int resultCode) {
        Log.d(TAG, "fetching result code is " + resultCode);
        switch (resultCode) {
            case FormsService.NO_NETWORK_CONNECTIVITY:
                Toast.makeText(this, "No network connectivity, please try again later", Toast.LENGTH_SHORT).show();
                break;
            case FormsService.ALREADY_FETCHING:
                Toast.makeText(this, "Already fetching forms, ignored the request", Toast.LENGTH_SHORT).show();
                break;
            case FormsService.IO_EXCEPTION:
                Toast.makeText(this, "I/O Exception occurred while fetching forms from server", Toast.LENGTH_SHORT).show();
                break;
            case FormsService.CONNECTION_TIMEOUT:
                Toast.makeText(this, "Connection timeout occurred while fetching forms from server", Toast.LENGTH_SHORT).show();
                break;
            case FormsService.JSON_EXCEPTION:
                Toast.makeText(this, "JSON Parse Exception occurred while fetching forms from server", Toast.LENGTH_SHORT).show();
                break;
            case FormsService.FETCH_SUCCESSFUL:
                Toast.makeText(this, "Done fetching forms", Toast.LENGTH_SHORT).show();
                handleNewFormsListVisibility();
                if (html5FormDataSource.hasForms()) {
                    html5FormsAdapter.dataSetChanged();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction transaction) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction transaction) {
        if (tab.equals(newTab)) {
            formsList.setAdapter(html5FormsAdapter);
            handleNewFormsListVisibility();
        } else if (tab.equals(incompleteTab)) {
            makeNoDataTextVisible(noIncompleteFormMsg, noIncompleteFormTip);
        } else if (tab.equals(completeTab)) {
            makeNoDataTextVisible(noCompleteFormMsg, noCompleteFormTip);
        } else if (tab.equals(syncedTab)) {
            makeNoDataTextVisible(noSyncedFormMsg, noSyncedFormTip);
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction transaction) {
    }

    private void initTabs() {
        newTab = getSupportActionBar().newTab();
        newTab.setText("New");
        newTab.setTabListener(this);
        getSupportActionBar().addTab(newTab);

        completeTab = getSupportActionBar().newTab();
        completeTab = getSupportActionBar().newTab();
        completeTab.setText("Completed");
        completeTab.setTabListener(this);
        getSupportActionBar().addTab(completeTab);

        incompleteTab = getSupportActionBar().newTab();
        incompleteTab = getSupportActionBar().newTab();
        incompleteTab.setText("Incomplete");
        incompleteTab.setTabListener(this);
        getSupportActionBar().addTab(incompleteTab);

        syncedTab = getSupportActionBar().newTab();
        syncedTab = getSupportActionBar().newTab();
        syncedTab.setText("Synced");
        syncedTab.setTabListener(this);
        getSupportActionBar().addTab(syncedTab);
    }

    private void handleNewFormsListVisibility() {
        if (html5FormDataSource.hasForms()) {
            makeListVisible();
        } else {
            makeNoDataTextVisible(noNewFormMsg, noNewFormTip);
        }
    }

    private void makeListVisible() {
        formsList.setVisibility(View.VISIBLE);
        noDataView.setVisibility(View.GONE);
    }

    private void makeNoDataTextVisible(String msg, String tip) {
        noDataView.setVisibility(View.VISIBLE);
        formsList.setVisibility(View.GONE);
        noDataText.setText(msg);
        noDataTip.setText(tip);
    }

    private void setupNoDataView() {
        noDataText = (TextView) findViewById(R.id.no_data_text);
        noDataTip = (TextView) findViewById(R.id.no_data_tip);
        noDataText.setTypeface(Fonts.roboto_bold_condensed(this));
        noDataTip.setTypeface(Fonts.roboto_light(this));

        noNewFormMsg = getResources().getString(R.string.no_new_form_msg);
        noNewFormTip = getResources().getString(R.string.no_new_form_tip);
        noIncompleteFormMsg = getResources().getString(R.string.no_incomplete_form_msg);
        noIncompleteFormTip = getResources().getString(R.string.no_incomplete_form_tip);
        noCompleteFormMsg = getResources().getString(R.string.no_complete_form_msg);
        noCompleteFormTip = getResources().getString(R.string.no_complete_form_tip);
        noSyncedFormMsg = getResources().getString(R.string.no_synced_form_msg);
        noSyncedFormTip = getResources().getString(R.string.no_synced_form_tip);
    }

}
