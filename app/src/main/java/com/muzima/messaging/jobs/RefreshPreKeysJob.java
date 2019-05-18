package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.crypto.IdentityKeyUtil;
import com.muzima.messaging.crypto.PreKeyUtil;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.jobmanager.dependencies.InjectableType;

import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class RefreshPreKeysJob extends ContextJob implements InjectableType {

    private static final String TAG = RefreshPreKeysJob.class.getSimpleName();

    private static final int PREKEY_MINIMUM = 10;

    @Inject
    transient SignalServiceAccountManager accountManager;

    public RefreshPreKeysJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public RefreshPreKeysJob(Context context) {
        super(context, JobParameters.newBuilder()
                .withGroupId(RefreshPreKeysJob.class.getSimpleName())
                .withNetworkRequirement()
                .withRetryCount(5)
                .create());
    }

    @Override
    protected void initialize(@NonNull SafeData data) {
    }

    @Override
    protected @NonNull
    Data serialize(@NonNull Data.Builder dataBuilder) {
        return dataBuilder.build();
    }

    @Override
    public void onRun() throws IOException {
        if (!TextSecurePreferences.isPushRegistered(context)) return;

        int availableKeys = accountManager.getPreKeysCount();

        if (availableKeys >= PREKEY_MINIMUM && TextSecurePreferences.isSignedPreKeyRegistered(context)) {
            Log.i(TAG, "Available keys sufficient: " + availableKeys);
            return;
        }

        List<PreKeyRecord> preKeyRecords = PreKeyUtil.generatePreKeys(context);
        IdentityKeyPair identityKey = IdentityKeyUtil.getIdentityKeyPair(context);
        SignedPreKeyRecord signedPreKeyRecord = PreKeyUtil.generateSignedPreKey(context, identityKey, false);

        Log.i(TAG, "Registering new prekeys...");

        accountManager.setPreKeys(identityKey.getPublicKey(), signedPreKeyRecord, preKeyRecords);

        PreKeyUtil.setActiveSignedPreKeyId(context, signedPreKeyRecord.getId());
        TextSecurePreferences.setSignedPreKeyRegistered(context, true);

        MuzimaApplication.getInstance(context)
                .getJobManager()
                .add(new CleanPreKeysJob(context));
    }

    @Override
    public boolean onShouldRetry(Exception exception) {
        if (exception instanceof NonSuccessfulResponseCodeException) return false;
        if (exception instanceof PushNetworkException) return true;

        return false;
    }

    @Override
    public void onCanceled() {

    }
}
