
package com.muzima;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;

public class HomeActivity extends SherlockActivity implements ActionBar.TabListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        initTabs();
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
