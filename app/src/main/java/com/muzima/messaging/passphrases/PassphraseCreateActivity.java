package com.muzima.messaging.passphrases;

import android.os.AsyncTask;
import android.os.Bundle;

import com.muzima.R;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.crypto.IdentityKeyUtil;
import com.muzima.messaging.crypto.MasterSecret;
import com.muzima.messaging.crypto.MasterSecretUtil;
import com.muzima.messaging.utils.Util;
import com.muzima.utils.VersionTracker;

public class PassphraseCreateActivity extends PassphraseActivity {

    public PassphraseCreateActivity() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.create_passphrase_activity);

        initializeResources();
    }

    private void initializeResources() {
        new SecretGenerator().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, MasterSecretUtil.UNENCRYPTED_PASSPHRASE);
    }

    private class SecretGenerator extends AsyncTask<String, Void, Void> {
        private MasterSecret masterSecret;

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(String... params) {
            String passphrase = params[0];
            masterSecret      = MasterSecretUtil.generateMasterSecret(PassphraseCreateActivity.this,
                    passphrase);

            MasterSecretUtil.generateAsymmetricMasterSecret(PassphraseCreateActivity.this, masterSecret);
            IdentityKeyUtil.generateIdentityKeys(PassphraseCreateActivity.this);
            VersionTracker.updateLastSeenVersion(PassphraseCreateActivity.this);

            TextSecurePreferences.setLastExperienceVersionCode(PassphraseCreateActivity.this, Util.getCurrentApkReleaseVersion(PassphraseCreateActivity.this));
            TextSecurePreferences.setPasswordDisabled(PassphraseCreateActivity.this, true);
            TextSecurePreferences.setReadReceiptsEnabled(PassphraseCreateActivity.this, true);
            TextSecurePreferences.setTypingIndicatorsEnabled(PassphraseCreateActivity.this, true);

            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            setMasterSecret(masterSecret);
        }
    }

    @Override
    protected void cleanup() {
        System.gc();
    }
}
