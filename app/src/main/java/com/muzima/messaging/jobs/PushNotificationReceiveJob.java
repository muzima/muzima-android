package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.muzima.R;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;

import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class PushNotificationReceiveJob extends PushReceivedJob {

    private static final String TAG = PushNotificationReceiveJob.class.getSimpleName();

    @Inject
    transient SignalServiceMessageReceiver receiver;

    public PushNotificationReceiveJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public PushNotificationReceiveJob(Context context) {
        super(context, JobParameters.newBuilder()
                .withNetworkRequirement()
                .withGroupId("__notification_received")
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
    protected String getDescription() {
        return context.getString(R.string.general_retrieving_a_message);
    }

    @Override
    public void onRun() throws IOException {
        pullAndProcessMessages(receiver, TAG, System.currentTimeMillis());
    }

    public void pullAndProcessMessages(SignalServiceMessageReceiver receiver, String tag, long startTime) throws IOException {
        synchronized (PushReceivedJob.RECEIVE_LOCK) {
            receiver.retrieveMessages(envelope -> {
                Log.i(tag, "Retrieved an envelope." + timeSuffix(startTime));
                processEnvelope(envelope);
                Log.i(tag, "Successfully processed an envelope." + timeSuffix(startTime));
            });
            TextSecurePreferences.setNeedsMessagePull(context, false);
        }
    }

    @Override
    public boolean onShouldRetry(Exception e) {
        Log.w(TAG, e);
        return e instanceof PushNetworkException;
    }

    @Override
    public void onCanceled() {
        Log.w(TAG, "***** Failed to download pending message!");
    }

    private static String timeSuffix(long startTime) {
        return " (" + (System.currentTimeMillis() - startTime) + " ms elapsed)";
    }
}
