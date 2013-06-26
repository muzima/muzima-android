
package com.muzima;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.muzima.domain.Form;
import com.muzima.domain.Html5Form;

public class FormsActivity extends SherlockActivity implements ActionBar.TabListener {
    private ListView formsList;
    private Form[] forms = {
            new Html5Form("1", "Patient Form", "A form to register patient", null),
            new Html5Form("2", "PMTCT Form", "", null),
            new Html5Form("1", "Ante-Natal Care Form", "This forms hold the ante-natal care information", null),};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        formsList = (ListView) findViewById(R.id.forms_list);
        formsList.setAdapter(new FormsAdapter(this, R.layout.form_list_item, forms));

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        initTabs();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SearchView searchView = new SearchView(getSupportActionBar().getThemedContext());
        searchView.setQueryHint("Search forms..");

        menu.add("Search")
                .setIcon(R.drawable.ic_search_inverse)
                .setActionView(searchView)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        return true;
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

}
