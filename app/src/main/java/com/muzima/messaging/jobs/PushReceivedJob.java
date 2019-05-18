package com.muzima.messaging.jobs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.MessagingDatabase.SyncMessageId;
import com.muzima.messaging.sqlite.database.RecipientDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.model.SignalRecipient;

import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;

import androidx.work.WorkerParameters;

public abstract class PushReceivedJob extends ContextJob {

    private static final String TAG = PushReceivedJob.class.getSimpleName();

    public static final Object RECEIVE_LOCK = new Object();

    protected PushReceivedJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    protected PushReceivedJob(Context context, JobParameters parameters) {
        super(context, parameters);
    }

    public void processEnvelope(@NonNull SignalServiceEnvelope envelope) {
        synchronized (RECEIVE_LOCK) {
            if (envelope.hasSource()) {
                SignalAddress source    = SignalAddress.fromExternal(context, envelope.getSource());
                SignalRecipient recipient = SignalRecipient.from(context, source, false);

                if (!isActiveNumber(recipient)) {
                    DatabaseFactory.getRecipientDatabase(context).setRegistered(recipient, RecipientDatabase.RegisteredState.REGISTERED);
                    MuzimaApplication.getInstance(context).getJobManager().add(new DirectoryRefreshJob(context, recipient, false));
                }
            }

            if (envelope.isReceipt()) {
                handleReceipt(envelope);
            } else if (envelope.isPreKeySignalMessage() || envelope.isSignalMessage() || envelope.isUnidentifiedSender()) {
                handleMessage(envelope);
            } else {
                Log.w(TAG, "Received envelope of unknown type: " + envelope.getType());
            }
        }
    }

    private void handleMessage(SignalServiceEnvelope envelope) {
        // TODO: Work on PushDecryptJob
        //new PushDecryptJob(context).processMessage(envelope);
    }

    @SuppressLint("DefaultLocale")
    private void handleReceipt(SignalServiceEnvelope envelope) {
        Log.i(TAG, String.format("Received receipt: (XXXXX, %d)", envelope.getTimestamp()));
        DatabaseFactory.getMmsSmsDatabase(context).incrementDeliveryReceiptCount(new SyncMessageId(SignalAddress.fromExternal(context, envelope.getSource()),
                envelope.getTimestamp()), System.currentTimeMillis());
    }

    private boolean isActiveNumber(@NonNull SignalRecipient recipient) {
        return recipient.resolve().getRegistered() == RecipientDatabase.RegisteredState.REGISTERED;
    }
}
