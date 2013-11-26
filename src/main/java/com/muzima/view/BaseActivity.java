package com.muzima.view;

import android.os.Bundle;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;

public class BaseActivity extends SherlockActivity {

    private DefaultMenuDropDownHelper dropDownHelper;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupActionBar();
        dropDownHelper = new DefaultMenuDropDownHelper(this);
    }

	private void setupActionBar() {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowTitleEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.dashboard, menu);
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
