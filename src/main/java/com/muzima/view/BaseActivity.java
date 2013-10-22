package com.muzima.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;
import com.muzima.domain.Credentials;
import com.muzima.search.api.util.StringUtil;
import com.muzima.view.login.LoginActivity;
import com.muzima.view.preferences.SettingsActivity;

public class BaseActivity extends SherlockFragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (new Credentials(this).isEmpty()) {
            launchLoginActivity(true);
        }
    }

    protected void launchLoginActivity(boolean isFirstLaunch) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.isFirstLaunch, isFirstLaunch);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_help:
                intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_logout:
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String passwordKey = getResources().getString(R.string.preference_password);
                settings.edit()
                        .putString(passwordKey, StringUtil.EMPTY)
                        .commit();

                launchLoginActivity(false);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void disableSettingsMenu(Menu menu) {
        MenuItem menuSettings = menu.findItem(R.id.action_settings);
        menuSettings.setEnabled(false);
    }
}
