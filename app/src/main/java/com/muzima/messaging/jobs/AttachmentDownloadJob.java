package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import com.muzima.messaging.attachments.Attachment;
import com.muzima.messaging.attachments.AttachmentId;
import com.muzima.messaging.attachments.DatabaseAttachment;
import com.muzima.messaging.events.PartProgressEvent;
import com.muzima.messaging.exceptions.MmsException;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.jobmanager.dependencies.InjectableType;
import com.muzima.messaging.sqlite.database.AttachmentDatabase;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.utils.AttachmentUtil;
import com.muzima.messaging.utils.Util;
import com.muzima.notifications.MessageNotifier;
import com.muzima.utils.Base64;

import org.greenrobot.eventbus.EventBus;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.util.Hex;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentPointer;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class AttachmentDownloadJob extends ContextJob implements InjectableType {
    private static final long   serialVersionUID    = 2L;
    private static final int    MAX_ATTACHMENT_SIZE = 150 * 1024  * 1024;
    private static final String TAG = AttachmentDownloadJob.class.getSimpleName();

    private static final String KEY_MESSAGE_ID    = "message_id";
    private static final String KEY_PART_ROW_ID   = "part_row_id";
    private static final String KEY_PAR_UNIQUE_ID = "part_unique_id";
    private static final String KEY_MANUAL        = "part_manual";

    @Inject
    transient SignalServiceMessageReceiver messageReceiver;

    private long    messageId;
    private long    partRowId;
    private long    partUniqueId;
    private boolean manual;

    public AttachmentDownloadJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public AttachmentDownloadJob(Context context, long messageId, AttachmentId attachmentId, boolean manual) {
        super(context, JobParameters.newBuilder()
                .withGroupId(AttachmentDownloadJob.class.getSimpleName() + attachmentId.getRowId() + "-" + attachmentId.getUniqueId())
                .withNetworkRequirement()
                .create());

        this.messageId    = messageId;
        this.partRowId    = attachmentId.getRowId();
        this.partUniqueId = attachmentId.getUniqueId();
        this.manual       = manual;
    }

    @Override
    protected void initialize(@NonNull SafeData data) {
        messageId    = data.getLong(KEY_MESSAGE_ID);
        partRowId    = data.getLong(KEY_PART_ROW_ID);
        partUniqueId = data.getLong(KEY_PAR_UNIQUE_ID);
        manual       = data.getBoolean(KEY_MANUAL);
    }

    @Override
    protected @NonNull
    Data serialize(@NonNull Data.Builder dataBuilder) {
        return dataBuilder.putLong(KEY_MESSAGE_ID, messageId)
                .putLong(KEY_PART_ROW_ID, partRowId)
                .putLong(KEY_PAR_UNIQUE_ID, partUniqueId)
                .putBoolean(KEY_MANUAL, manual)
                .build();
    }

    @Override
    public void onAdded() {
        Log.i(TAG, "onAdded() messageId: " + messageId + "  partRowId: " + partRowId + "  partUniqueId: " + partUniqueId + "  manual: " + manual);

        final AttachmentDatabase database     = DatabaseFactory.getAttachmentDatabase(context);
        final AttachmentId       attachmentId = new AttachmentId(partRowId, partUniqueId);
        final DatabaseAttachment attachment   = database.getAttachment(attachmentId);
        final boolean            pending      = attachment != null && attachment.getTransferState() != AttachmentDatabase.TRANSFER_PROGRESS_DONE;

        if (pending && (manual || AttachmentUtil.isAutoDownloadPermitted(context, attachment))) {
            Log.i(TAG, "onAdded() Marking attachment progress as 'started'");
            database.setTransferState(messageId, attachmentId, AttachmentDatabase.TRANSFER_PROGRESS_STARTED);
        }
    }

    @Override
    public void onRun() throws IOException {
        Log.i(TAG, "onRun() messageId: " + messageId + "  partRowId: " + partRowId + "  partUniqueId: " + partUniqueId + "  manual: " + manual);

        final AttachmentDatabase database     = DatabaseFactory.getAttachmentDatabase(context);
        final AttachmentId       attachmentId = new AttachmentId(partRowId, partUniqueId);
        final DatabaseAttachment attachment   = database.getAttachment(attachmentId);

        if (attachment == null) {
            Log.w(TAG, "attachment no longer exists.");
            return;
        }

        if (!attachment.isInProgress()) {
            Log.w(TAG, "Attachment was already downloaded.");
            return;
        }

        if (!manual && !AttachmentUtil.isAutoDownloadPermitted(context, attachment)) {
            Log.w(TAG, "Attachment can't be auto downloaded...");
            database.setTransferState(messageId, attachmentId, AttachmentDatabase.TRANSFER_PROGRESS_PENDING);
            return;
        }

        Log.i(TAG, "Downloading push part " + attachmentId);
        database.setTransferState(messageId, attachmentId, AttachmentDatabase.TRANSFER_PROGRESS_STARTED);

        retrieveAttachment(messageId, attachmentId, attachment);
        MessageNotifier.updateNotification(context);
    }

    @Override
    public void onCanceled() {
        Log.w(TAG, "onCanceled() messageId: " + messageId + "  partRowId: " + partRowId + "  partUniqueId: " + partUniqueId + "  manual: " + manual);

        final AttachmentId attachmentId = new AttachmentId(partRowId, partUniqueId);
        markFailed(messageId, attachmentId);
    }

    @Override
    protected boolean onShouldRetry(Exception exception) {
        return (exception instanceof PushNetworkException);
    }

    private void retrieveAttachment(long messageId,
                                    final AttachmentId attachmentId,
                                    final Attachment attachment)
            throws IOException
    {

        AttachmentDatabase database       = DatabaseFactory.getAttachmentDatabase(context);
        File attachmentFile = null;

        try {
            attachmentFile = createTempFile();

            SignalServiceAttachmentPointer pointer = createAttachmentPointer(attachment);
            InputStream stream  = messageReceiver.retrieveAttachment(pointer, attachmentFile, MAX_ATTACHMENT_SIZE, (total, progress) -> EventBus.getDefault().postSticky(new PartProgressEvent(attachment, total, progress)));

            database.insertAttachmentsForPlaceholder(messageId, attachmentId, stream);
        } catch (InvalidPartException | NonSuccessfulResponseCodeException | InvalidMessageException | MmsException e) {
            Log.w(TAG, "Experienced exception while trying to download an attachment.", e);
            markFailed(messageId, attachmentId);
        } finally {
            if (attachmentFile != null) {
                //noinspection ResultOfMethodCallIgnored
                attachmentFile.delete();
            }
        }
    }

    @VisibleForTesting
    SignalServiceAttachmentPointer createAttachmentPointer(Attachment attachment)
            throws InvalidPartException
    {
        if (TextUtils.isEmpty(attachment.getLocation())) {
            throw new InvalidPartException("empty content id");
        }

        if (TextUtils.isEmpty(attachment.getKey())) {
            throw new InvalidPartException("empty encrypted key");
        }

        try {
            long   id    = Long.parseLong(attachment.getLocation());
            byte[] key   = Base64.decode(attachment.getKey());
            String relay = null;

            if (TextUtils.isEmpty(attachment.getRelay())) {
                relay = attachment.getRelay();
            }

            if (attachment.getDigest() != null) {
                Log.i(TAG, "Downloading attachment with digest: " + Hex.toString(attachment.getDigest()));
            } else {
                Log.i(TAG, "Downloading attachment with no digest...");
            }

            return new SignalServiceAttachmentPointer(id, null, key,
                    Optional.of(Util.toIntExact(attachment.getSize())),
                    Optional.absent(),
                    0, 0,
                    Optional.fromNullable(attachment.getDigest()),
                    Optional.fromNullable(attachment.getFileName()),
                    attachment.isVoiceNote(),
                    Optional.absent());
        } catch (IOException | ArithmeticException e) {
            Log.w(TAG, e);
            throw new InvalidPartException(e);
        }
    }

    private File createTempFile() throws InvalidPartException {
        try {
            File file = File.createTempFile("push-attachment", "tmp", context.getCacheDir());
            file.deleteOnExit();

            return file;
        } catch (IOException e) {
            throw new InvalidPartException(e);
        }
    }

    private void markFailed(long messageId, AttachmentId attachmentId) {
        try {
            AttachmentDatabase database = DatabaseFactory.getAttachmentDatabase(context);
            database.setTransferProgressFailed(attachmentId, messageId);
        } catch (MmsException e) {
            Log.w(TAG, e);
        }
    }

    @VisibleForTesting
    static class InvalidPartException extends Exception {
        InvalidPartException(String s) {super(s);}
        InvalidPartException(Exception e) {super(e);}
    }
}
