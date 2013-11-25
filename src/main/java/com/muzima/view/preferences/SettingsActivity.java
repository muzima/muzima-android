package com.muzima.view.preferences;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.muzima.R;
import com.muzima.api.context.ContextFactory;
import com.muzima.domain.Credentials;
import com.muzima.search.api.util.StringUtil;
import com.muzima.service.CohortPrefixPreferenceService;
import com.muzima.service.ConceptPreferenceService;
import com.muzima.service.CredentialsPreferenceService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.tasks.ValidateURLTask;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.login.LoginActivity;

import java.io.File;

public class SettingsActivity extends SherlockPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private String serverPreferenceKey;
    private String usernamePreferenceKey;
    private String passwordPreferenceKey;

    private EditTextPreference serverPreference;
    private EditTextPreference usernamePreference;
    private EditTextPreference passwordPreference;

    private ProgressDialog progressDialog;
    private String newURL;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
        serverPreferenceKey = getResources().getString(R.string.preference_server);
        serverPreference = (EditTextPreference) getPreferenceScreen().findPreference(serverPreferenceKey);
        serverPreference.setSummary(serverPreference.getText());

        serverPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                newURL =newValue.toString();
                if (!serverPreference.getText().equalsIgnoreCase(newURL)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                    builder
                            .setCancelable(true)
                            .setIcon(getResources().getDrawable(R.drawable.ic_warning))
                            .setTitle(getResources().getString(R.string.caution))
                            .setMessage(getResources().getString(R.string.switch_server_message))
                            .setPositiveButton("Yes", positiveClickListener())
                            .setNegativeButton("No", null).create().show();
                }
                return false;
            }
        });
        usernamePreferenceKey = getResources().getString(R.string.preference_username);
        usernamePreference = (EditTextPreference) getPreferenceScreen().findPreference(usernamePreferenceKey);
        usernamePreference.setSummary(usernamePreference.getText());

        passwordPreferenceKey = getResources().getString(R.string.preference_password);
        passwordPreference = (EditTextPreference) getPreferenceScreen().findPreference(passwordPreferenceKey);
        if (passwordPreference.getText() != null) {
            passwordPreference.setSummary(passwordPreference.getText().replaceAll(".", "*"));
        }

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
        if (key.equalsIgnoreCase("wizardFinished")) {
            return;
        }
        String value = sharedPreferences.getString(key, StringUtil.EMPTY);
        if (StringUtil.equals(key, serverPreferenceKey)) {
            serverPreference.setSummary(value);
        } else if (StringUtil.equals(key, usernamePreferenceKey)) {
            usernamePreference.setSummary(value);
        } else if (StringUtil.equals(key, passwordPreferenceKey)) {
            passwordPreference.setSummary(value.replaceAll(".", "*"));
        }
    }

    public void validationURLResult(boolean result) {
        if (result) {
            new ResetDataTask(this).execute();
        } else {
            progressDialog.dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                    .setCancelable(true)
                    .setIcon(getResources().getDrawable(R.drawable.ic_warning))
                    .setTitle("Invalid")
                    .setMessage("The URL you have provided is invalid")
                    .setPositiveButton("Ok", null);
            builder.create().show();
        }
    }

    private Dialog.OnClickListener positiveClickListener() {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                changeServerURL(dialog);
            }

        };
    }


    private void changeServerURL(DialogInterface dialog) {
        dialog.dismiss();
        if (NetworkUtils.isConnectedToNetwork(this)) {
            new ValidateURLTask(this).execute(newURL);
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void resetData() {
        clearApplicationData();
        SettingsActivity context = SettingsActivity.this;
        new WizardFinishPreferenceService(context).resetWizard();
        new CredentialsPreferenceService(context).saveCredentials(new Credentials(newURL, null, null));
        new ConceptPreferenceService(context).clearConcepts();
        new CohortPrefixPreferenceService(context).clearPrefixes();
    }

    private void clearApplicationData() {
        try {
            File dir = new File(ContextFactory.APP_DIR);
            if (dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear the application data", e);
        }
    }


    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    protected void launchLoginActivity(boolean isFirstLaunch) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.isFirstLaunch, isFirstLaunch);
        startActivity(intent);
        finish();
    }

    private class ResetDataTask extends AsyncTask<String, Void, Void> {
        private SettingsActivity settingsActivity;

        public ResetDataTask(SettingsActivity settingsActivity) {
            this.settingsActivity = settingsActivity;
        }

        @Override
        protected Void doInBackground(String... params) {
            settingsActivity.resetData();
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(settingsActivity);
            progressDialog.setMessage("Step 2: Resetting Data");
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Void v) {
            progressDialog.dismiss();
            super.onPostExecute(v);
            launchLoginActivity(true);
        }
    }
}
