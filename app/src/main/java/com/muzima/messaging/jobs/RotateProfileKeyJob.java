package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.muzima.MuzimaApplication;
import com.muzima.messaging.AvatarHelper;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.crypto.ProfileKeyUtil;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.jobmanager.dependencies.InjectableType;
import com.muzima.messaging.sqlite.database.SignalAddress;

import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;
import org.whispersystems.signalservice.api.util.StreamDetails;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class RotateProfileKeyJob extends ContextJob implements InjectableType {

    @Inject
    SignalServiceAccountManager accountManager;

    public RotateProfileKeyJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public RotateProfileKeyJob(Context context) {
        super(context, new JobParameters.Builder()
                .withGroupId("__ROTATE_PROFILE_KEY__")
                .withDuplicatesIgnored(true)
                .withNetworkRequirement()
                .create());
    }

    @NonNull
    @Override
    protected Data serialize(@NonNull Data.Builder dataBuilder) {
        return dataBuilder.build();
    }

    @Override
    protected void initialize(@NonNull SafeData data) {
    }

    @Override
    public void onRun() throws Exception {
        byte[] profileKey = ProfileKeyUtil.rotateProfileKey(context);

        accountManager.setProfileName(profileKey, TextSecurePreferences.getProfileName(context));
        accountManager.setProfileAvatar(profileKey, getProfileAvatar());

        MuzimaApplication.getInstance(context)
                .getJobManager()
                .add(new RefreshAttributesJob(context));
    }

    @Override
    protected void onCanceled() {

    }

    @Override
    protected boolean onShouldRetry(Exception exception) {
        return exception instanceof PushNetworkException;
    }

    private @Nullable
    StreamDetails getProfileAvatar() {
        try {
            SignalAddress localAddress = SignalAddress.fromSerialized(TextSecurePreferences.getLocalNumber(context));
            File avatarFile   = AvatarHelper.getAvatarFile(context, localAddress);

            if (avatarFile.exists()) {
                return new StreamDetails(new FileInputStream(avatarFile), "image/jpeg", avatarFile.length());
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }
}
