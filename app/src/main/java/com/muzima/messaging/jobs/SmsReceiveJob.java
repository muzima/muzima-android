package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.SmsMessage;
import android.util.Log;

import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.sms.IncomingTextMessage;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.MessagingDatabase.InsertResult;
import com.muzima.messaging.sqlite.database.SmsDatabase;
import com.muzima.model.SignalRecipient;
import com.muzima.notifications.MessageNotifier;
import com.muzima.utils.Base64;

import org.whispersystems.libsignal.util.guava.Optional;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class SmsReceiveJob extends ContextJob {

    private static final long serialVersionUID = 1L;

    private static final String TAG = SmsReceiveJob.class.getSimpleName();

    private static final String KEY_PDUS            = "pdus";
    private static final String KEY_SUBSCRIPTION_ID = "subscription_id";

    private @Nullable Object[] pdus;

    private int subscriptionId;

    public SmsReceiveJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public SmsReceiveJob(@NonNull Context context, @Nullable Object[] pdus, int subscriptionId) {
        super(context, JobParameters.newBuilder()
                .withSqlCipherRequirement()
                .create());

        this.pdus           = pdus;
        this.subscriptionId = subscriptionId;
    }

    @Override
    protected void initialize(@NonNull SafeData data) {
        String[] encoded = data.getStringArray(KEY_PDUS);
        pdus = new Object[encoded.length];
        try {
            for (int i = 0; i < encoded.length; i++) {
                pdus[i] = Base64.decode(encoded[i]);
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        subscriptionId = data.getInt(KEY_SUBSCRIPTION_ID);
    }

    @Override
    protected @NonNull
    Data serialize(@NonNull Data.Builder dataBuilder) {
        String[] encoded = new String[pdus.length];
        for (int i = 0; i < pdus.length; i++) {
            encoded[i] = Base64.encodeBytes((byte[]) pdus[i]);
        }

        return dataBuilder.putStringArray(KEY_PDUS, encoded)
                .putInt(KEY_SUBSCRIPTION_ID, subscriptionId)
                .build();
    }

    @Override
    public void onRun() throws MigrationPendingException {
        Log.i(TAG, "onRun()");

        Optional<IncomingTextMessage> message = assembleMessageFragments(pdus, subscriptionId);

        if (message.isPresent() && !isBlocked(message.get())) {
            Optional<InsertResult> insertResult = storeMessage(message.get());

            if (insertResult.isPresent()) {
                MessageNotifier.updateNotification(context, insertResult.get().getThreadId());
            }
        } else if (message.isPresent()) {
            Log.w(TAG, "*** Received blocked SMS, ignoring...");
        } else {
            Log.w(TAG, "*** Failed to assemble message fragments!");
        }
    }

    @Override
    public void onCanceled() {

    }

    @Override
    public boolean onShouldRetry(Exception exception) {
        return exception instanceof MigrationPendingException;
    }

    private boolean isBlocked(IncomingTextMessage message) {
        if (message.getSender() != null) {
            SignalRecipient recipient = SignalRecipient.from(context, message.getSender(), false);
            return recipient.isBlocked();
        }

        return false;
    }

    private Optional<InsertResult> storeMessage(IncomingTextMessage message) throws MigrationPendingException {
        SmsDatabase database = DatabaseFactory.getSmsDatabase(context);
        database.ensureMigration();

        if (TextSecurePreferences.getNeedsSqlCipherMigration(context)) {
            throw new MigrationPendingException();
        }

        if (message.isSecureMessage()) {
            IncomingTextMessage    placeholder  = new IncomingTextMessage(message, "");
            Optional<InsertResult> insertResult = database.insertMessageInbox(placeholder);
            database.markAsLegacyVersion(insertResult.get().getMessageId());

            return insertResult;
        } else {
            return database.insertMessageInbox(message);
        }
    }

    private Optional<IncomingTextMessage> assembleMessageFragments(@Nullable Object[] pdus, int subscriptionId) {
        if (pdus == null) {
            return Optional.absent();
        }

        List<IncomingTextMessage> messages = new LinkedList<>();

        for (Object pdu : pdus) {
            messages.add(new IncomingTextMessage(context, SmsMessage.createFromPdu((byte[])pdu), subscriptionId));
        }

        if (messages.isEmpty()) {
            return Optional.absent();
        }

        return Optional.of(new IncomingTextMessage(messages));
    }

    private class MigrationPendingException extends Exception {

    }
}
