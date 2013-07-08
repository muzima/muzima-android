
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
import com.actionbarsherlock.widget.SearchView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.db.Html5FormDataSource;
import com.muzima.service.FormsService;
import com.muzima.utils.Fonts;


public class FormsActivity extends SherlockActivity implements ActionBar.TabListener, FormsService.OnDataFetchComplete {
    private static final String TAG = "FormsActivity";
    private ListView formsList;
    private FormsService formsService;
    private Html5FormDataSource html5FormDataSource;
    private Html5FormsAdapter html5FormsAdapter;
    private View noDataView;

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
        setupListView();

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        initTabs();
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
                setupListView();
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
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction transaction) {
    }

    private void initTabs() {
        ActionBar.Tab tab = getSupportActionBar().newTab();
        tab.setText("New");
        tab.setTabListener(this);
        getSupportActionBar().addTab(tab);

        tab = getSupportActionBar().newTab();
        tab.setText("Incomplete");
        tab.setTabListener(this);
        getSupportActionBar().addTab(tab);

        tab = getSupportActionBar().newTab();
        tab.setText("Completed");
        tab.setTabListener(this);
        getSupportActionBar().addTab(tab);

        tab = getSupportActionBar().newTab();
        tab.setText("Synced");
        tab.setTabListener(this);
        getSupportActionBar().addTab(tab);
    }

    private void setupListView() {
        if (html5FormDataSource.hasForms()) {
            formsList.setVisibility(View.VISIBLE);
            noDataView.setVisibility(View.GONE);
            formsList.setAdapter(html5FormsAdapter);
        } else {
            noDataView.setVisibility(View.VISIBLE);
            formsList.setVisibility(View.GONE);
        }
    }

    private void setupNoDataView() {
        TextView noDataText = (TextView) findViewById(R.id.no_data_text);
        TextView noDataTip = (TextView) findViewById(R.id.no_data_tip);
        noDataText.setTypeface(Fonts.roboto_bold_condensed(this));
        noDataTip.setTypeface(Fonts.roboto_light(this));
    }

}
