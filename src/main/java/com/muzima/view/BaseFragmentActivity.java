package com.muzima.view;

import android.os.Bundle;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.domain.Credentials;

public class BaseFragmentActivity extends SherlockFragmentActivity {

    private DefaultMenuDropDownHelper dropDownHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBar();
        dropDownHelper = new DefaultMenuDropDownHelper(this);
    }

    private void setActionBar() {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowTitleEnabled(true);
        }
    }

    @Override
    public void onUserInteraction() {
        ((MuzimaApplication) getApplication()).restartTimer();
        super.onUserInteraction();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((MuzimaApplication) getApplication()).setCurrentActivity(this);
        if (new Credentials(this).isEmpty()) {
            dropDownHelper.launchLoginActivity(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(DefaultMenuDropDownHelper.DEFAULT_MENU, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = dropDownHelper.onOptionsItemSelected(item);
        return result || super.onOptionsItemSelected(item);
    }

    protected void removeSettingsMenu(Menu menu) {
        dropDownHelper.removeSettingsMenu(menu);
    }
}
