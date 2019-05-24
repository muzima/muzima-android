package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.crypto.UnidentifiedAccessUtil;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.jobmanager.dependencies.InjectableType;

import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.NetworkFailureException;

import java.io.IOException;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class RefreshAttributesJob extends ContextJob implements InjectableType {

    public static final long serialVersionUID = 1L;

    private static final String TAG = RefreshAttributesJob.class.getSimpleName();

    @Inject
    transient SignalServiceAccountManager signalAccountManager;

    public RefreshAttributesJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public RefreshAttributesJob(Context context) {
        super(context, JobParameters.newBuilder()
                .withNetworkRequirement()
                .withGroupId(RefreshAttributesJob.class.getName())
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
        String  signalingKey                = TextSecurePreferences.getSignalingKey(context);
        int     registrationId              = TextSecurePreferences.getLocalRegistrationId(context);
        boolean fetchesMessages             = TextSecurePreferences.isGcmDisabled(context);
        String  pin                         = TextSecurePreferences.getRegistrationLockPin(context);
        byte[]  unidentifiedAccessKey       = UnidentifiedAccessUtil.getSelfUnidentifiedAccessKey(context);
        boolean universalUnidentifiedAccess = TextSecurePreferences.isUniversalUnidentifiedAccess(context);

        signalAccountManager.setAccountAttributes(signalingKey, registrationId, fetchesMessages, pin,
                unidentifiedAccessKey, universalUnidentifiedAccess);

        MuzimaApplication.getInstance(context)
                .getJobManager()
                .add(new RefreshUnidentifiedDeliveryAbilityJob(context));
    }

    @Override
    public boolean onShouldRetry(Exception e) {
        return e instanceof NetworkFailureException;
    }

    @Override
    public void onCanceled() {
        Log.w(TAG, "Failed to update account attributes!");
    }
}
