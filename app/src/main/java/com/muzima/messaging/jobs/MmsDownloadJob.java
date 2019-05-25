package com.muzima.messaging.jobs;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.mms.pdu_alt.CharacterSets;
import com.google.android.mms.pdu_alt.EncodedStringValue;
import com.google.android.mms.pdu_alt.PduBody;
import com.google.android.mms.pdu_alt.PduPart;
import com.google.android.mms.pdu_alt.RetrieveConf;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.attachments.Attachment;
import com.muzima.messaging.attachments.UriAttachment;
import com.muzima.messaging.exceptions.ApnUnavailableException;
import com.muzima.messaging.exceptions.MmsException;
import com.muzima.messaging.exceptions.MmsRadioException;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.mms.CompatMmsConnection;
import com.muzima.messaging.mms.IncomingMediaMessage;
import com.muzima.messaging.mms.PartParser;
import com.muzima.messaging.provider.MemoryBlobProvider;
import com.muzima.messaging.sqlite.database.AttachmentDatabase;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.MessagingDatabase.InsertResult;
import com.muzima.messaging.sqlite.database.MmsDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.utils.Util;
import com.muzima.notifications.MessageNotifier;
import com.muzima.service.KeyCachingService;

import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.LegacyMessageException;
import org.whispersystems.libsignal.NoSessionException;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class MmsDownloadJob extends ContextJob {

    private static final long serialVersionUID = 1L;

    private static final String TAG = MmsDownloadJob.class.getSimpleName();

    private static final String KEY_MESSAGE_ID = "message_id";
    private static final String KEY_THREAD_ID = "thread_id";
    private static final String KEY_AUTOMATIC = "automatic";

    private long messageId;
    private long threadId;
    private boolean automatic;

    public MmsDownloadJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public MmsDownloadJob(Context context, long messageId, long threadId, boolean automatic) {
        super(context, JobParameters.newBuilder()
                .withGroupId("mms-operation")
                .create());

        this.messageId = messageId;
        this.threadId = threadId;
        this.automatic = automatic;
    }

    @Override
    protected void initialize(@NonNull SafeData data) {
        messageId = data.getLong(KEY_MESSAGE_ID);
        threadId = data.getLong(KEY_THREAD_ID);
        automatic = data.getBoolean(KEY_AUTOMATIC);
    }

    @Override
    protected @NonNull
    Data serialize(@NonNull Data.Builder dataBuilder) {
        return dataBuilder.putLong(KEY_MESSAGE_ID, messageId)
                .putLong(KEY_THREAD_ID, threadId)
                .putBoolean(KEY_AUTOMATIC, automatic)
                .build();
    }

    @Override
    public void onAdded() {
        if (automatic && KeyCachingService.isLocked(context)) {
            DatabaseFactory.getMmsDatabase(context).markIncomingNotificationReceived(threadId);
            MessageNotifier.updateNotification(context);
        }
    }

    @Override
    public void onRun() {
        MmsDatabase database = DatabaseFactory.getMmsDatabase(context);
        Optional<MmsDatabase.MmsNotificationInfo> notification = database.getNotification(messageId);

        if (!notification.isPresent()) {
            Log.w(TAG, "No notification for ID: " + messageId);
            return;
        }

        try {
            if (notification.get().getContentLocation() == null) {
                throw new MmsException("Notification content location was null.");
            }

            if (!TextSecurePreferences.isPushRegistered(context)) {
                throw new MmsException("Not registered");
            }

            database.markDownloadState(messageId, MmsDatabase.Status.DOWNLOAD_CONNECTING);

            String contentLocation = notification.get().getContentLocation();
            byte[] transactionId = new byte[0];

            try {
                if (notification.get().getTransactionId() != null) {
                    transactionId = notification.get().getTransactionId().getBytes(CharacterSets.MIMENAME_ISO_8859_1);
                } else {
                    Log.w(TAG, "No transaction ID!");
                }
            } catch (UnsupportedEncodingException e) {
                Log.w(TAG, e);
            }

            Log.i(TAG, "Downloading mms at " + Uri.parse(contentLocation).getHost() + ", subscription ID: " + notification.get().getSubscriptionId());

            RetrieveConf retrieveConf = new CompatMmsConnection(context).retrieve(contentLocation, transactionId, notification.get().getSubscriptionId());

            if (retrieveConf == null) {
                throw new MmsException("RetrieveConf was null");
            }

            storeRetrievedMms(contentLocation, messageId, threadId, retrieveConf, notification.get().getSubscriptionId(), notification.get().getFrom());
        } catch (ApnUnavailableException e) {
            Log.w(TAG, e);
            handleDownloadError(messageId, threadId, MmsDatabase.Status.DOWNLOAD_APN_UNAVAILABLE,
                    automatic);
        } catch (MmsException e) {
            Log.w(TAG, e);
            handleDownloadError(messageId, threadId,
                    MmsDatabase.Status.DOWNLOAD_HARD_FAILURE,
                    automatic);
        } catch (MmsRadioException | IOException e) {
            Log.w(TAG, e);
            handleDownloadError(messageId, threadId,
                    MmsDatabase.Status.DOWNLOAD_SOFT_FAILURE,
                    automatic);
        } catch (DuplicateMessageException e) {
            Log.w(TAG, e);
            database.markAsDecryptDuplicate(messageId, threadId);
        } catch (LegacyMessageException e) {
            Log.w(TAG, e);
            database.markAsLegacyVersion(messageId, threadId);
        } catch (NoSessionException e) {
            Log.w(TAG, e);
            database.markAsNoSession(messageId, threadId);
        } catch (InvalidMessageException e) {
            Log.w(TAG, e);
            database.markAsDecryptFailed(messageId, threadId);
        }
    }

    @Override
    public void onCanceled() {
        MmsDatabase database = DatabaseFactory.getMmsDatabase(context);
        database.markDownloadState(messageId, MmsDatabase.Status.DOWNLOAD_SOFT_FAILURE);

        if (automatic) {
            database.markIncomingNotificationReceived(threadId);
            MessageNotifier.updateNotification(context, threadId);
        }
    }

    @Override
    public boolean onShouldRetry(Exception exception) {
        return false;
    }

    private void storeRetrievedMms(String contentLocation,
                                   long messageId, long threadId, RetrieveConf retrieved,
                                   int subscriptionId, @Nullable SignalAddress notificationFrom)
            throws MmsException, NoSessionException, DuplicateMessageException, InvalidMessageException,
            LegacyMessageException {
        MmsDatabase database = DatabaseFactory.getMmsDatabase(context);
        MemoryBlobProvider provider = MemoryBlobProvider.getInstance();
        Optional<SignalAddress> group = Optional.absent();
        Set<SignalAddress> members = new HashSet<>();
        String body = null;
        List<Attachment> attachments = new LinkedList<>();

        SignalAddress from;

        if (retrieved.getFrom() != null) {
            from = SignalAddress.fromExternal(context, Util.toIsoString(retrieved.getFrom().getTextString()));
        } else if (notificationFrom != null) {
            from = notificationFrom;
        } else {
            from = SignalAddress.UNKNOWN;
        }

        if (retrieved.getTo() != null) {
            for (EncodedStringValue toValue : retrieved.getTo()) {
                members.add(SignalAddress.fromExternal(context, Util.toIsoString(toValue.getTextString())));
            }
        }

        if (retrieved.getCc() != null) {
            for (EncodedStringValue ccValue : retrieved.getCc()) {
                members.add(SignalAddress.fromExternal(context, Util.toIsoString(ccValue.getTextString())));
            }
        }

        members.add(from);
        members.add(SignalAddress.fromExternal(context, TextSecurePreferences.getLocalNumber(context)));

        if (retrieved.getBody() != null) {
            body = PartParser.getMessageText(retrieved.getBody());
            PduBody media = PartParser.getSupportedMediaParts(retrieved.getBody());

            for (int i = 0; i < media.getPartsNum(); i++) {
                PduPart part = media.getPart(i);

                if (part.getData() != null) {
                    Uri uri = provider.createSingleUseUri(part.getData());
                    String name = null;

                    if (part.getName() != null) name = Util.toIsoString(part.getName());

                    attachments.add(new UriAttachment(uri, Util.toIsoString(part.getContentType()),
                            AttachmentDatabase.TRANSFER_PROGRESS_DONE,
                            part.getData().length, name, false, false, null));
                }
            }
        }

        if (members.size() > 2) {
            group = Optional.of(SignalAddress.fromSerialized(DatabaseFactory.getGroupDatabase(context).getOrCreateGroupForMembers(new LinkedList<>(members), true)));
        }

        IncomingMediaMessage message = new IncomingMediaMessage(from, group, body, retrieved.getDate() * 1000L, attachments, subscriptionId, 0, false, false);
        Optional<InsertResult> insertResult = database.insertMessageInbox(message, contentLocation, threadId);

        if (insertResult.isPresent()) {
            database.delete(messageId);
            MessageNotifier.updateNotification(context, insertResult.get().getThreadId());
        }
    }

    private void handleDownloadError(long messageId, long threadId, int downloadStatus, boolean automatic) {
        MmsDatabase db = DatabaseFactory.getMmsDatabase(context);

        db.markDownloadState(messageId, downloadStatus);

        if (automatic) {
            db.markIncomingNotificationReceived(threadId);
            MessageNotifier.updateNotification(context, threadId);
        }
    }
}
