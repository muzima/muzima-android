package com.muzima.messaging.jobs;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.muzima.R;
import com.muzima.messaging.PlayServicesProblemActivity;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.jobmanager.dependencies.InjectableType;
import com.muzima.notifications.NotificationChannels;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class GcmRefreshJob extends ContextJob implements InjectableType {

    private static final String TAG = GcmRefreshJob.class.getSimpleName();

    public static final String REGISTRATION_ID = "312334754206";

    @Inject
    transient SignalServiceAccountManager textSecureAccountManager;

    public GcmRefreshJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public GcmRefreshJob(Context context) {
        super(context, JobParameters.newBuilder()
                .withGroupId(GcmRefreshJob.class.getSimpleName())
                .withDuplicatesIgnored(true)
                .withNetworkRequirement()
                .withRetryCount(1)
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
    public void onRun() throws Exception {
        if (TextSecurePreferences.isGcmDisabled(context)) return;

        Log.i(TAG, "Reregistering GCM...");
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);

        if (result != ConnectionResult.SUCCESS) {
            notifyGcmFailure();
        } else {
            String gcmId = GoogleCloudMessaging.getInstance(context).register(REGISTRATION_ID);
            textSecureAccountManager.setGcmId(Optional.of(gcmId));

            TextSecurePreferences.setGcmRegistrationId(context, gcmId);
            TextSecurePreferences.setGcmRegistrationIdLastSetTime(context, System.currentTimeMillis());
            TextSecurePreferences.setWebsocketRegistered(context, true);
        }
    }

    @Override
    public void onCanceled() {
        Log.w(TAG, "GCM reregistration failed after retry attempt exhaustion!");
    }

    @Override
    public boolean onShouldRetry(Exception throwable) {
        if (throwable instanceof NonSuccessfulResponseCodeException) return false;
        return true;
    }

    private void notifyGcmFailure() {
        Intent intent = new Intent(context, PlayServicesProblemActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1122, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannels.FAILURES);

        builder.setSmallIcon(R.drawable.ic_launcher_logo_light);
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_action_warning_red));
        builder.setContentTitle(context.getString(R.string.warning_signal_communication_failure));
        builder.setContentText(context.getString(R.string.warning_unable_to_register_with_Google_Play_Services));
        builder.setTicker(context.getString(R.string.warning_signal_communication_failure));
        builder.setVibrate(new long[] {0, 1000});
        builder.setContentIntent(pendingIntent);

        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(12, builder.build());
    }
}
