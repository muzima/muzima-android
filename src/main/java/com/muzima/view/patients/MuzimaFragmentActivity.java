package com.muzima.view.patients;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.BroadcastListenerActivity;
import com.muzima.R;
import com.muzima.search.api.util.StringUtil;

//TODO pull the onOptionsItemClick up here
public class MuzimaFragmentActivity extends BroadcastListenerActivity {
    public MuzimaFragmentActivity() {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return false;
        }
    }

    public String[] getCredentials() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String usernameKey = getResources().getString(R.string.preference_username);
        String passwordKey = getResources().getString(R.string.preference_password);
        String serverKey = getResources().getString(R.string.preference_server);
        String[] credentials = new String[]{settings.getString(usernameKey, StringUtil.EMPTY),
                settings.getString(passwordKey, StringUtil.EMPTY),
                settings.getString(serverKey, StringUtil.EMPTY)};
        return credentials;
    }
}
