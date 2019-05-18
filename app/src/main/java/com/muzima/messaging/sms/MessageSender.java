package com.muzima.messaging.sms;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import com.muzima.MuzimaApplication;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.attachments.Attachment;
import com.muzima.messaging.exceptions.MmsException;
import com.muzima.messaging.jobmanager.JobManager;
import com.muzima.messaging.jobs.PushGroupSendJob;
import com.muzima.messaging.jobs.PushMediaSendJob;
import com.muzima.messaging.jobs.PushTextSendJob;
import com.muzima.messaging.mms.OutgoingMediaMessage;
import com.muzima.messaging.push.AccountManagerFactory;
import com.muzima.messaging.sqlite.database.AttachmentDatabase;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.MmsDatabase;
import com.muzima.messaging.sqlite.database.RecipientDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.sqlite.database.SmsDatabase;
import com.muzima.messaging.sqlite.database.ThreadDatabase;
import com.muzima.messaging.sqlite.database.models.MessageRecord;
import com.muzima.messaging.sqlite.database.models.MmsMessageRecord;
import com.muzima.messaging.utils.Util;
import com.muzima.model.SignalRecipient;
import com.muzima.service.ExpiringMessageManager;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.ContactTokenDetails;

import java.io.IOException;

public class MessageSender {
    private static final String TAG = MessageSender.class.getSimpleName();

    public static long send(final Context context,
                            final OutgoingTextMessage message,
                            final long threadId,
                            final boolean forceSms,
                            final SmsDatabase.InsertListener insertListener) {
        SmsDatabase database = DatabaseFactory.getSmsDatabase(context);
        SignalRecipient recipient = message.getRecipient();
        boolean keyExchange = message.isKeyExchange();

        long allocatedThreadId;

        if (threadId == -1) {
            allocatedThreadId = DatabaseFactory.getThreadDatabase(context).getThreadIdFor(recipient);
        } else {
            allocatedThreadId = threadId;
        }

        long messageId = database.insertMessageOutbox(allocatedThreadId, message, forceSms, System.currentTimeMillis(), insertListener);

        sendTextMessage(context, recipient, forceSms, keyExchange, messageId, message.getExpiresIn());

        return allocatedThreadId;
    }

    public static long send(final Context context,
                            final OutgoingMediaMessage message,
                            final long threadId,
                            final boolean forceSms,
                            final SmsDatabase.InsertListener insertListener) {
        try {
            ThreadDatabase threadDatabase = DatabaseFactory.getThreadDatabase(context);
            MmsDatabase database = DatabaseFactory.getMmsDatabase(context);

            long allocatedThreadId;

            if (threadId == -1) {
                allocatedThreadId = threadDatabase.getThreadIdFor(message.getRecipient(), message.getDistributionType());
            } else {
                allocatedThreadId = threadId;
            }

            SignalRecipient recipient = message.getRecipient();
            long messageId = database.insertMessageOutbox(message, allocatedThreadId, forceSms, insertListener);

            sendMediaMessage(context, recipient, forceSms, messageId, message.getExpiresIn());

            return allocatedThreadId;
        } catch (MmsException e) {
            Log.w(TAG, e);
            return threadId;
        }
    }

    public static void resendGroupMessage(Context context, MessageRecord messageRecord, SignalAddress filterAddress) {
        if (!messageRecord.isMms()) throw new AssertionError("Not Group");
        sendGroupPush(context, messageRecord.getRecipient(), messageRecord.getId(), filterAddress);
    }

    public static void resend(Context context, MessageRecord messageRecord) {
        try {
            long messageId = messageRecord.getId();
            boolean forceSms = messageRecord.isForcedSms();
            boolean keyExchange = messageRecord.isKeyExchange();
            long expiresIn = messageRecord.getExpiresIn();
            SignalRecipient recipient = messageRecord.getRecipient();

            if (messageRecord.isMms()) {
                sendMediaMessage(context, recipient, forceSms, messageId, expiresIn);
            } else {
                sendTextMessage(context, recipient, forceSms, keyExchange, messageId, expiresIn);
            }
        } catch (MmsException e) {
            Log.w(TAG, e);
        }
    }

    private static void sendMediaMessage(Context context, SignalRecipient recipient, boolean forceSms, long messageId, long expiresIn)
            throws MmsException {
        if (!forceSms && isSelfSend(context, recipient)) {
            sendMediaSelf(context, messageId, expiresIn);
        } else if (isGroupPushSend(recipient)) {
            sendGroupPush(context, recipient, messageId, null);
        } else if (!forceSms && isPushMediaSend(context, recipient)) {
            sendMediaPush(context, recipient, messageId);
        } else {
            sendMms(context, messageId);
        }
    }

    private static void sendTextMessage(Context context, SignalRecipient recipient,
                                        boolean forceSms, boolean keyExchange,
                                        long messageId, long expiresIn) {
        if (!forceSms && isSelfSend(context, recipient)) {
            sendTextSelf(context, messageId, expiresIn);
        } else if (!forceSms && isPushTextSend(context, recipient, keyExchange)) {
            sendTextPush(context, recipient, messageId);
        } else {
            sendSms(context, recipient, messageId);
        }
    }

    private static void sendTextSelf(Context context, long messageId, long expiresIn) {
        SmsDatabase database = DatabaseFactory.getSmsDatabase(context);

        database.markAsSent(messageId, true);

        Pair<Long, Long> messageAndThreadId = database.copyMessageInbox(messageId);
        database.markAsPush(messageAndThreadId.first);

        if (expiresIn > 0) {
            ExpiringMessageManager expiringMessageManager = MuzimaApplication.getInstance(context).getExpiringMessageManager();

            database.markExpireStarted(messageId);
            expiringMessageManager.scheduleDeletion(messageId, false, expiresIn);
        }
    }

    private static void sendMediaSelf(Context context, long messageId, long expiresIn)
            throws MmsException {
        ExpiringMessageManager expiringMessageManager = MuzimaApplication.getInstance(context).getExpiringMessageManager();
        MmsDatabase database = DatabaseFactory.getMmsDatabase(context);

        database.markAsSent(messageId, true);
        database.copyMessageInbox(messageId);
        markAttachmentsAsUploaded(messageId, database, DatabaseFactory.getAttachmentDatabase(context));

        if (expiresIn > 0) {
            database.markExpireStarted(messageId);
            expiringMessageManager.scheduleDeletion(messageId, true, expiresIn);
        }
    }

    private static void sendTextPush(Context context, SignalRecipient recipient, long messageId) {
        JobManager jobManager = MuzimaApplication.getInstance(context).getJobManager();
        jobManager.add(new PushTextSendJob(context, messageId, recipient.getAddress()));
    }

    private static void sendMediaPush(Context context, SignalRecipient recipient, long messageId) {
        JobManager jobManager = MuzimaApplication.getInstance(context).getJobManager();
        jobManager.add(new PushMediaSendJob(context, messageId, recipient.getAddress()));
    }

    private static void sendGroupPush(Context context, SignalRecipient recipient, long messageId, SignalAddress filterAddress) {
        JobManager jobManager = MuzimaApplication.getInstance(context).getJobManager();
        jobManager.add(new PushGroupSendJob(context, messageId, recipient.getAddress(), filterAddress));
    }

    private static void sendSms(Context context, SignalRecipient recipient, long messageId) {
        JobManager jobManager = MuzimaApplication.getInstance(context).getJobManager();
        //todo work on smssendjob
        //jobManager.add(new SmsSendJob(context, messageId, recipient.getName()));
    }

    private static void sendMms(Context context, long messageId) {
        JobManager jobManager = MuzimaApplication.getInstance(context).getJobManager();
       //Todo work on mmssend
        // jobManager.add(new MmsSendJob(context, messageId));
    }

    private static boolean isPushTextSend(Context context, SignalRecipient recipient, boolean keyExchange) {
        if (!TextSecurePreferences.isPushRegistered(context)) {
            return false;
        }

        if (keyExchange) {
            return false;
        }

        return isPushDestination(context, recipient);
    }

    private static boolean isPushMediaSend(Context context, SignalRecipient recipient) {
        if (!TextSecurePreferences.isPushRegistered(context)) {
            return false;
        }

        if (recipient.isGroupRecipient()) {
            return false;
        }

        return isPushDestination(context, recipient);
    }

    private static boolean isGroupPushSend(SignalRecipient recipient) {
        return recipient.getAddress().isGroup() &&
                !recipient.getAddress().isMmsGroup();
    }

    private static boolean isSelfSend(Context context, SignalRecipient recipient) {
        if (!TextSecurePreferences.isPushRegistered(context)) {
            return false;
        }

        if (recipient.isGroupRecipient()) {
            return false;
        }

        return Util.isOwnNumber(context, recipient.getAddress());
    }

    private static boolean isPushDestination(Context context, SignalRecipient destination) {
        if (destination.resolve().getRegistered() == RecipientDatabase.RegisteredState.REGISTERED) {
            return true;
        } else if (destination.resolve().getRegistered() == RecipientDatabase.RegisteredState.NOT_REGISTERED) {
            return false;
        } else {
            try {
                SignalServiceAccountManager accountManager = AccountManagerFactory.createManager(context);
                Optional<ContactTokenDetails> registeredUser = accountManager.getContact(destination.getAddress().serialize());

                if (!registeredUser.isPresent()) {
                    DatabaseFactory.getRecipientDatabase(context).setRegistered(destination, RecipientDatabase.RegisteredState.NOT_REGISTERED);
                    return false;
                } else {
                    DatabaseFactory.getRecipientDatabase(context).setRegistered(destination, RecipientDatabase.RegisteredState.REGISTERED);
                    return true;
                }
            } catch (IOException e1) {
                Log.w(TAG, e1);
                return false;
            }
        }
    }

    private static void markAttachmentsAsUploaded(long mmsId, @NonNull MmsDatabase mmsDatabase, @NonNull AttachmentDatabase attachmentDatabase) {
        try (MmsDatabase.Reader reader = mmsDatabase.readerFor(mmsDatabase.getMessage(mmsId))) {
            MessageRecord message = reader.getNext();

            if (message != null && message.isMms()) {
                for (Attachment attachment : ((MmsMessageRecord) message).getSlideDeck().asAttachments()) {
                    attachmentDatabase.markAttachmentUploaded(mmsId, attachment);
                }
            }
        }
    }
}
