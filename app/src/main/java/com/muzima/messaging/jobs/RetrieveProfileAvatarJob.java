package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.muzima.messaging.AvatarHelper;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.jobmanager.dependencies.InjectableType;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.RecipientDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.utils.Util;
import com.muzima.model.SignalRecipient;

import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class RetrieveProfileAvatarJob extends ContextJob implements InjectableType {

    private static final String TAG = RetrieveProfileAvatarJob.class.getSimpleName();

    private static final int MAX_PROFILE_SIZE_BYTES = 20 * 1024 * 1024;

    private static final String KEY_PROFILE_AVATAR = "profile_avatar";
    private static final String KEY_ADDRESS        = "address";

    @Inject
    SignalServiceMessageReceiver receiver;

    private String    profileAvatar;
    private SignalRecipient recipient;

    public RetrieveProfileAvatarJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public RetrieveProfileAvatarJob(Context context, SignalRecipient recipient, String profileAvatar) {
        super(context, JobParameters.newBuilder()
                .withGroupId(RetrieveProfileAvatarJob.class.getSimpleName() + recipient.getAddress().serialize())
                .withDuplicatesIgnored(true)
                .withNetworkRequirement()
                .create());

        this.recipient     = recipient;
        this.profileAvatar = profileAvatar;
    }

    @Override
    protected void initialize(@NonNull SafeData data) {
        profileAvatar = data.getString(KEY_PROFILE_AVATAR);
        recipient     = SignalRecipient.from(context, SignalAddress.fromSerialized(data.getString(KEY_ADDRESS)), true);
    }

    @Override
    protected @NonNull
    Data serialize(@NonNull Data.Builder dataBuilder) {
        return dataBuilder.putString(KEY_PROFILE_AVATAR, profileAvatar)
                .putString(KEY_ADDRESS, recipient.getAddress().serialize())
                .build();
    }

    @Override
    public void onRun() throws IOException {
        RecipientDatabase database   = DatabaseFactory.getRecipientDatabase(context);
        byte[]            profileKey = recipient.resolve().getProfileKey();

        if (profileKey == null) {
            Log.w(TAG, "SignalRecipient profile key is gone!");
            return;
        }

        if (Util.equals(profileAvatar, recipient.resolve().getProfileAvatar())) {
            Log.w(TAG, "Already retrieved profile avatar: " + profileAvatar);
            return;
        }

        if (TextUtils.isEmpty(profileAvatar)) {
            Log.w(TAG, "Removing profile avatar for: " + recipient.getAddress().serialize());
            AvatarHelper.delete(context, recipient.getAddress());
            database.setProfileAvatar(recipient, profileAvatar);
            return;
        }

        File downloadDestination = File.createTempFile("avatar", "jpg", context.getCacheDir());

        try {
            InputStream avatarStream       = receiver.retrieveProfileAvatar(profileAvatar, downloadDestination, profileKey, MAX_PROFILE_SIZE_BYTES);
            File        decryptDestination = File.createTempFile("avatar", "jpg", context.getCacheDir());

            Util.copy(avatarStream, new FileOutputStream(decryptDestination));
            decryptDestination.renameTo(AvatarHelper.getAvatarFile(context, recipient.getAddress()));
        } finally {
            if (downloadDestination != null) downloadDestination.delete();
        }

        database.setProfileAvatar(recipient, profileAvatar);
    }

    @Override
    public boolean onShouldRetry(Exception e) {
        Log.w(TAG, e);
        if (e instanceof PushNetworkException) return true;
        return false;
    }

    @Override
    public void onCanceled() {

    }
}
