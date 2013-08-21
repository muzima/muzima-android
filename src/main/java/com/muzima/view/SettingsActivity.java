package com.muzima.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.muzima.R;
import com.muzima.search.api.util.StringUtil;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private String serverPreferenceKey;
    private String usernamePreferenceKey;
    private String passwordPreferenceKey;
    private String cohortPrefixPreferenceKey;

    private EditTextPreference serverPreference;
    private EditTextPreference usernamePreference;
    private EditTextPreference passwordPreference;
    private EditTextPreference cohortPrefixPreference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);

        serverPreferenceKey = getResources().getString(R.string.preference_server);
        serverPreference = (EditTextPreference) getPreferenceScreen().findPreference(serverPreferenceKey);
        serverPreference.setSummary(serverPreference.getText());

        usernamePreferenceKey = getResources().getString(R.string.preference_username);
        usernamePreference = (EditTextPreference) getPreferenceScreen().findPreference(usernamePreferenceKey);
        usernamePreference.setSummary(usernamePreference.getText());

        passwordPreferenceKey = getResources().getString(R.string.preference_password);
        passwordPreference = (EditTextPreference) getPreferenceScreen().findPreference(passwordPreferenceKey);
        if (passwordPreference.getText() != null) {
            passwordPreference.setSummary(passwordPreference.getText().replaceAll(".", "*"));
        }

        cohortPrefixPreferenceKey = getResources().getString(R.string.preference_cohort_prefix);
        cohortPrefixPreference = (EditTextPreference) getPreferenceScreen().findPreference(cohortPrefixPreferenceKey);
        cohortPrefixPreference.setSummary(cohortPrefixPreference.getText());

        // Show the Up button in the action bar.
        setupActionBar();
    }

    /**
     * Called when a shared preference is changed, added, or removed. This
     * may be called even if a preference is set to its existing value.
     * <p/>
     * <p>This callback will be run on your main thread.
     *
     * @param sharedPreferences The {@link android.content.SharedPreferences} that received
     *                          the change.
     * @param key               The key of the preference that was changed, added, or
     *                          removed.
     */
    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        String value = sharedPreferences.getString(key, StringUtil.EMPTY);
        if (StringUtil.equals(key, serverPreferenceKey)) {
            serverPreference.setSummary(value);
        } else if (StringUtil.equals(key, usernamePreferenceKey)) {
            usernamePreference.setSummary(value);
        } else if (StringUtil.equals(key, passwordPreferenceKey)) {
            passwordPreference.setSummary(value.replaceAll(".", "*"));
        } else if(StringUtil.equals(key, cohortPrefixPreferenceKey)){
            cohortPrefixPreference.setSummary(value);
        }
    }

    /**
     * Called after {@link #onRestoreInstanceState}, {@link #onRestart}, or
     * {@link #onPause}, for your activity to start interacting with the user.
     * This is a good place to begin animations, open exclusive-access devices
     * (such as the camera), etc.
     * <p/>
     * <p>Keep in mind that onResume is not the best indicator that your activity
     * is visible to the user; a system window such as the keyguard may be in
     * front.  Use {@link #onWindowFocusChanged} to know for certain that your
     * activity is visible to the user (for example, to resume a game).
     * <p/>
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @see #onRestoreInstanceState
     * @see #onRestart
     * @see #onPostResume
     * @see #onPause
     */
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
