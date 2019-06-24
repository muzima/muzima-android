package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.sqlite.database.DatabaseFactory;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class TrimThreadJob extends ContextJob{
    private static final String TAG = TrimThreadJob.class.getSimpleName();

    private static final String KEY_THREAD_ID = "thread_id";

    private long threadId;

    public TrimThreadJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public TrimThreadJob(Context context, long threadId) {
        super(context, JobParameters.newBuilder().withGroupId(TrimThreadJob.class.getSimpleName()).create());
        this.context = context;
        this.threadId = threadId;
    }

    @Override
    protected void initialize(@NonNull SafeData data) {
        threadId = data.getLong(KEY_THREAD_ID);
    }

    @Override
    protected @NonNull
    Data serialize(@NonNull Data.Builder dataBuilder) {
        return dataBuilder.putLong(KEY_THREAD_ID, threadId).build();
    }

    @Override
    public void onRun() {
        boolean trimmingEnabled   = TextSecurePreferences.isThreadLengthTrimmingEnabled(context);
        int     threadLengthLimit = TextSecurePreferences.getThreadTrimLength(context);

        if (!trimmingEnabled)
            return;

        DatabaseFactory.getThreadDatabase(context).trimThread(threadId, threadLengthLimit);
    }

    @Override
    public boolean onShouldRetry(Exception exception) {
        return false;
    }

    @Override
    public void onCanceled() {
        Log.w(TAG, "Canceling trim attempt: " + threadId);
    }
}
