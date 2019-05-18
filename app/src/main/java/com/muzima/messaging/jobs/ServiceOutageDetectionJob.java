package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.muzima.BuildConfig;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.exceptions.RetryLaterException;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;

import org.greenrobot.eventbus.EventBus;

import java.net.InetAddress;
import java.net.UnknownHostException;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class ServiceOutageDetectionJob extends ContextJob {

    private static final String TAG = ServiceOutageDetectionJob.class.getSimpleName();

    private static final String IP_SUCCESS = "127.0.0.1";
    private static final String IP_FAILURE = "127.0.0.2";
    private static final long   CHECK_TIME = 1000 * 60;

    public ServiceOutageDetectionJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public ServiceOutageDetectionJob(Context context) {
        super(context, new JobParameters.Builder()
                .withGroupId(ServiceOutageDetectionJob.class.getSimpleName())
                .withDuplicatesIgnored(true)
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
    public void onRun() throws RetryLaterException {
        Log.i(TAG, "onRun()");

        long timeSinceLastCheck = System.currentTimeMillis() - TextSecurePreferences.getLastOutageCheckTime(context);
        if (timeSinceLastCheck < CHECK_TIME) {
            Log.w(TAG, "Skipping service outage check. Too soon.");
            return;
        }

        try {
            InetAddress address = InetAddress.getByName(BuildConfig.SIGNAL_SERVICE_STATUS_URL);
            Log.i(TAG, "Received outage check address: " + address.getHostAddress());

            if (IP_SUCCESS.equals(address.getHostAddress())) {
                Log.i(TAG, "Service is available.");
                TextSecurePreferences.setServiceOutage(context, false);
            } else if (IP_FAILURE.equals(address.getHostAddress())) {
                Log.w(TAG, "Service is down.");
                TextSecurePreferences.setServiceOutage(context, true);
            } else {
                Log.w(TAG, "Service status check returned an unrecognized IP address. Could be a weird network state. Prompting retry.");
                throw new RetryLaterException(new Exception("Unrecognized service outage IP address."));
            }

            TextSecurePreferences.setLastOutageCheckTime(context, System.currentTimeMillis());
        } catch (UnknownHostException e) {
            throw new RetryLaterException(e);
        }
    }

    @Override
    public boolean onShouldRetry(Exception e) {
        return e instanceof RetryLaterException;
    }

    @Override
    public void onCanceled() {
        Log.i(TAG, "Service status check could not complete. Assuming success to avoid false positives due to bad network.");
        TextSecurePreferences.setServiceOutage(context, false);
        TextSecurePreferences.setLastOutageCheckTime(context, System.currentTimeMillis());
    }
}
