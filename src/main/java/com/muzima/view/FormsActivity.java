
package com.muzima.view;

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
import com.muzima.R;
import com.muzima.db.Html5FormDataSource;
import com.muzima.service.FormsService;
import com.muzima.utils.Fonts;

import static com.muzima.db.Html5FormDataSource.DataChangeListener;

public class FormsActivity extends SherlockActivity implements ActionBar.TabListener, DataChangeListener {
    private static final String TAG = "FormsActivity";
    private ListView formsList;
    private FormsService formsService;
    private Html5FormDataSource html5FormDataSource;
    private Html5FormsAdapter html5FormsAdapter;
    private View noDataView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        formsService = ((MuzimaApplication) getApplication()).getFormsService();

        html5FormDataSource = ((MuzimaApplication) getApplication()).getHtml5FormDataSource();
        html5FormDataSource.open();
        html5FormDataSource.setDataChangeListener(this);
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
        getSupportMenuInflater().inflate(R.menu.form_list_activity_menu, menu);

        SearchView searchView = new SearchView(getSupportActionBar().getThemedContext());
        searchView.setQueryHint("Search forms..");

        menu.add("Search")
                .setIcon(R.drawable.ic_search)
                .setActionView(searchView)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_load:
                int fetchingStatus = formsService.fetchForms();
                Log.d(TAG, "fetching status is " + fetchingStatus);
                if (fetchingStatus == FormsService.NO_NETWORK_CONNECTIVITY) {
                    Toast.makeText(this, "No network connectivity, please try again later", Toast.LENGTH_SHORT).show();
                } else if (fetchingStatus == FormsService.ALREADY_FETCHING) {
                    Toast.makeText(this, "Already fetching forms, ignored the request", Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onInsert() {
        setupListView();
        if(html5FormDataSource.hasForms()){
            html5FormsAdapter.dataSetChanged();
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
        tab.setText("New Forms");
        tab.setTabListener(this);
        getSupportActionBar().addTab(tab);

        tab = getSupportActionBar().newTab();
        tab.setText("Drafts");
        tab.setTabListener(this);
        getSupportActionBar().addTab(tab);

        tab = getSupportActionBar().newTab();
        tab.setText("Synced Forms");
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
