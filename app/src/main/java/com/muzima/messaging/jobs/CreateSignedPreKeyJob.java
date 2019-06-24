package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.crypto.IdentityKeyUtil;
import com.muzima.messaging.crypto.PreKeyUtil;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.jobmanager.dependencies.InjectableType;

import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class CreateSignedPreKeyJob extends ContextJob implements InjectableType {

    private static final long serialVersionUID = 1L;

    private static final String TAG = CreateSignedPreKeyJob.class.getSimpleName();

    @Inject
    transient SignalServiceAccountManager accountManager;

    public CreateSignedPreKeyJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public CreateSignedPreKeyJob(Context context) {
        super(context, JobParameters.newBuilder()
                .withNetworkRequirement()
                .withGroupId(CreateSignedPreKeyJob.class.getSimpleName())
                .create());
    }

    @Override
    protected void initialize(@NonNull SafeData data) {
    }

    @Override
    protected @NonNull Data serialize(@NonNull Data.Builder dataBuilder) {
        return dataBuilder.build();
    }

    @Override
    public void onRun() throws IOException {
        if (TextSecurePreferences.isSignedPreKeyRegistered(context)) {
            Log.w(TAG, "Signed prekey already registered...");
            return;
        }

        if (!TextSecurePreferences.isPushRegistered(context)) {
            Log.w(TAG, "Not yet registered...");
            return;
        }

        IdentityKeyPair identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(context);
        SignedPreKeyRecord signedPreKeyRecord = PreKeyUtil.generateSignedPreKey(context, identityKeyPair, true);

        accountManager.setSignedPreKey(signedPreKeyRecord);
        TextSecurePreferences.setSignedPreKeyRegistered(context, true);
    }

    @Override
    public void onCanceled() {}

    @Override
    public boolean onShouldRetry(Exception exception) {
        if (exception instanceof PushNetworkException) return true;
        return false;
    }
}
