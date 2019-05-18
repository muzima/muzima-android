package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.crypto.UnidentifiedAccessUtil;
import com.muzima.messaging.exceptions.InsecureFallbackApprovalException;
import com.muzima.messaging.exceptions.RetryLaterException;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.jobmanager.dependencies.InjectableType;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.NoSuchMessageException;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.sqlite.database.SmsDatabase;
import com.muzima.messaging.sqlite.database.models.SmsMessageRecord;
import com.muzima.model.SignalRecipient;
import com.muzima.notifications.MessageNotifier;
import com.muzima.service.ExpiringMessageManager;
import com.muzima.messaging.sqlite.database.RecipientDatabase.UnidentifiedAccessMode;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UnidentifiedAccessPair;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.exceptions.UnregisteredUserException;

import java.io.IOException;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class PushTextSendJob extends PushSendJob implements InjectableType {

    private static final long serialVersionUID = 1L;

    private static final String TAG = PushTextSendJob.class.getSimpleName();

    private static final String KEY_MESSAGE_ID = "message_id";

    @Inject
    transient SignalServiceMessageSender messageSender;

    private long messageId;

    public PushTextSendJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public PushTextSendJob(Context context, long messageId, SignalAddress destination) {
        super(context, constructParameters(destination));
        this.messageId = messageId;
    }

    @Override
    protected void initialize(@NonNull SafeData data) {
        messageId = data.getLong(KEY_MESSAGE_ID);
    }

    @Override
    protected @NonNull
    Data serialize(@NonNull Data.Builder dataBuilder) {
        return dataBuilder.putLong(KEY_MESSAGE_ID, messageId).build();
    }

    @Override
    public void onAdded() {
        DatabaseFactory.getSmsDatabase(context).markAsSending(messageId);
    }

    @Override
    public void onPushSend() throws NoSuchMessageException, RetryLaterException {
        ExpiringMessageManager expirationManager = MuzimaApplication.getInstance(context).getExpiringMessageManager();
        SmsDatabase database = DatabaseFactory.getSmsDatabase(context);
        SmsMessageRecord record = database.getMessage(messageId);

        if (!record.isPending() && !record.isFailed()) {
            Log.w(TAG, "Message " + messageId + " was already sent. Ignoring.");
            return;
        }

        try {
            Log.e(TAG, "Sending message: "+messageId);

            SignalRecipient recipient = record.getRecipient().resolve();
            byte[] profileKey = recipient.getProfileKey();
            UnidentifiedAccessMode accessMode = recipient.getUnidentifiedAccessMode();

            boolean unidentified = deliver(record);

            database.markAsSent(messageId, true);
            database.markUnidentified(messageId, unidentified);

            if (TextSecurePreferences.isUnidentifiedDeliveryEnabled(context)) {
                if (unidentified && accessMode == UnidentifiedAccessMode.UNKNOWN && profileKey == null) {
                    Log.e(TAG, "Marking recipient as UD-unrestricted following a UD send.");
                    DatabaseFactory.getRecipientDatabase(context).setUnidentifiedAccessMode(recipient, UnidentifiedAccessMode.UNRESTRICTED);
                } else if (unidentified && accessMode == UnidentifiedAccessMode.UNKNOWN) {
                    Log.e(TAG, "Marking recipient as UD-enabled following a UD send.");
                    DatabaseFactory.getRecipientDatabase(context).setUnidentifiedAccessMode(recipient, UnidentifiedAccessMode.ENABLED);
                } else if (!unidentified && accessMode != UnidentifiedAccessMode.DISABLED) {
                    Log.e(TAG, "Marking recipient as UD-disabled following a non-UD send.");
                    DatabaseFactory.getRecipientDatabase(context).setUnidentifiedAccessMode(recipient, UnidentifiedAccessMode.DISABLED);
                }
            }

            if (record.getExpiresIn() > 0) {
                database.markExpireStarted(messageId);
                expirationManager.scheduleDeletion(record.getId(), record.isMms(), record.getExpiresIn());
            }

            Log.e(TAG, "Sent message: " + messageId);

        } catch (InsecureFallbackApprovalException e) {
            Log.w(TAG, "Failure", e);
            database.markAsPendingInsecureSmsFallback(record.getId());
            MessageNotifier.notifyMessageDeliveryFailed(context, record.getRecipient(), record.getThreadId());
            MuzimaApplication.getInstance(context).getJobManager().add(new DirectoryRefreshJob(context, false));
        } catch (UntrustedIdentityException e) {
            Log.w(TAG, "Failure", e);
            database.addMismatchedIdentity(record.getId(), SignalAddress.fromSerialized(e.getE164Number()), e.getIdentityKey());
            database.markAsSentFailed(record.getId());
            database.markAsPush(record.getId());
        }
    }

    @Override
    public boolean onShouldRetry(Exception exception) {
        if (exception instanceof RetryLaterException) return true;

        return false;
    }

    @Override
    public void onCanceled() {
        DatabaseFactory.getSmsDatabase(context).markAsSentFailed(messageId);

        long threadId = DatabaseFactory.getSmsDatabase(context).getThreadIdForMessage(messageId);
        SignalRecipient recipient = DatabaseFactory.getThreadDatabase(context).getRecipientForThreadId(threadId);

        if (threadId != -1 && recipient != null) {
            MessageNotifier.notifyMessageDeliveryFailed(context, recipient, threadId);
        }
    }

    private boolean deliver(SmsMessageRecord message)
            throws UntrustedIdentityException, InsecureFallbackApprovalException, RetryLaterException {
        try {
            rotateSenderCertificateIfNecessary();

            SignalServiceAddress address = getPushAddress(message.getIndividualRecipient().getAddress());
            Optional<byte[]> profileKey = getProfileKey(message.getIndividualRecipient());
            Optional<UnidentifiedAccessPair> unidentifiedAccess = UnidentifiedAccessUtil.getAccessFor(context, message.getIndividualRecipient());

            log(TAG, "Have access key to use: " + unidentifiedAccess.isPresent());

            SignalServiceDataMessage textSecureMessage = SignalServiceDataMessage.newBuilder()
                    .withTimestamp(message.getDateSent())
                    .withBody(message.getBody())
                    .withExpiration((int) (message.getExpiresIn() / 1000))
                    .withProfileKey(profileKey.orNull())
                    .asEndSessionMessage(message.isEndSession())
                    .build();

            return messageSender.sendMessage(address, unidentifiedAccess, textSecureMessage).getSuccess().isUnidentified();
        } catch (UnregisteredUserException e) {
            warn(TAG, "Failure", e);
            throw new InsecureFallbackApprovalException(e);
        } catch (IOException e) {
            warn(TAG, "Failure", e);
            throw new RetryLaterException(e);
        }
    }
}
