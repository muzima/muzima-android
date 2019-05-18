package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.attachments.Attachment;
import com.muzima.messaging.contactshare.Contact;
import com.muzima.messaging.contactshare.ContactModelMapper;
import com.muzima.messaging.crypto.ProfileKeyUtil;
import com.muzima.messaging.events.PartProgressEvent;
import com.muzima.messaging.exceptions.BitmapDecodingException;
import com.muzima.messaging.exceptions.TextSecureExpiredException;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.mms.DecryptableStreamUriLoader;
import com.muzima.messaging.mms.OutgoingMediaMessage;
import com.muzima.messaging.mms.PartAuthority;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.model.SignalRecipient;
import com.muzima.notifications.MessageNotifier;
import com.muzima.utils.BitmapUtil;
import com.muzima.utils.MediaUtil;

import org.greenrobot.eventbus.EventBus;
import org.signal.libsignal.metadata.certificate.InvalidCertificateException;
import org.signal.libsignal.metadata.certificate.SenderCertificate;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.shared.SharedContact;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.work.WorkerParameters;

public abstract class PushSendJob extends SendJob {

    private static final long serialVersionUID = 5906098204770900739L;
    private static final String TAG = PushSendJob.class.getSimpleName();
    private static final long CERTIFICATE_EXPIRATION_BUFFER = TimeUnit.DAYS.toMillis(1);

    protected PushSendJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    protected PushSendJob(Context context, JobParameters parameters) {
        super(context, parameters);
    }

    protected static JobParameters constructParameters(SignalAddress destination) {
        JobParameters.Builder builder = JobParameters.newBuilder();
        builder.withGroupId(destination.serialize());
        builder.withNetworkRequirement();
        builder.withRetryDuration(TimeUnit.DAYS.toMillis(1));

        return builder.create();
    }

    @Override
    protected final void onSend() throws Exception {
        if (TextSecurePreferences.getSignedPreKeyFailureCount(context) > 5) {
            MuzimaApplication.getInstance(context)
                    .getJobManager()
                    .add(new RotateSignedPreKeyJob(context));

            throw new TextSecureExpiredException("Too many signed prekey rotation failures");
        }

        onPushSend();
    }

    @Override
    public void onRetry() {
        super.onRetry();
        Log.i(TAG, "onRetry()");

        if (getRunAttemptCount() > 1) {
            Log.i(TAG, "Scheduling service outage detection job.");
            MuzimaApplication.getInstance(context).getJobManager().add(new ServiceOutageDetectionJob(context));
        }
    }

    protected Optional<byte[]> getProfileKey(@NonNull SignalRecipient recipient) {
        if (!recipient.resolve().isSystemContact() && !recipient.resolve().isProfileSharing()) {
            return Optional.absent();
        }

        return Optional.of(ProfileKeyUtil.getProfileKey(context));
    }

    protected SignalServiceAddress getPushAddress(SignalAddress address) {
        String relay = null;
        return new SignalServiceAddress(address.toPhoneString(), Optional.fromNullable(relay));
    }

    protected List<SignalServiceAttachment> getAttachmentsFor(List<Attachment> parts) {
        List<SignalServiceAttachment> attachments = new LinkedList<>();

        for (final Attachment attachment : parts) {
            SignalServiceAttachment converted = getAttachmentFor(attachment);
            if (converted != null) {
                attachments.add(converted);
            }
        }

        return attachments;
    }

    protected SignalServiceAttachment getAttachmentFor(Attachment attachment) {
        try {
            if (attachment.getDataUri() == null || attachment.getSize() == 0)
                throw new IOException("Assertion failed, outgoing attachment has no data!");
            InputStream is = PartAuthority.getAttachmentStream(context, attachment.getDataUri());
            return SignalServiceAttachment.newStreamBuilder()
                    .withStream(is)
                    .withContentType(attachment.getContentType())
                    .withLength(attachment.getSize())
                    .withFileName(attachment.getFileName())
                    .withVoiceNote(attachment.isVoiceNote())
                    .withWidth(attachment.getWidth())
                    .withHeight(attachment.getHeight())
                    .withCaption(attachment.getCaption())
                    .withListener((total, progress) -> EventBus.getDefault().postSticky(new PartProgressEvent(attachment, total, progress)))
                    .build();
        } catch (IOException ioe) {
            Log.w(TAG, "Couldn't open attachment", ioe);
        }
        return null;
    }

    protected void notifyMediaMessageDeliveryFailed(Context context, long messageId) {
        long threadId = DatabaseFactory.getMmsDatabase(context).getThreadIdForMessage(messageId);
        SignalRecipient recipient = DatabaseFactory.getThreadDatabase(context).getRecipientForThreadId(threadId);

        if (threadId != -1 && recipient != null) {
            MessageNotifier.notifyMessageDeliveryFailed(context, recipient, threadId);
        }
    }

    protected Optional<SignalServiceDataMessage.Quote> getQuoteFor(OutgoingMediaMessage message) {
        if (message.getOutgoingQuote() == null) return Optional.absent();

        long quoteId = message.getOutgoingQuote().getId();
        String quoteBody = message.getOutgoingQuote().getText();
        SignalAddress quoteAuthor = message.getOutgoingQuote().getAuthor();
        List<SignalServiceDataMessage.Quote.QuotedAttachment> quoteAttachments = new LinkedList<>();

        for (Attachment attachment : message.getOutgoingQuote().getAttachments()) {
            BitmapUtil.ScaleResult thumbnailData = null;
            SignalServiceAttachment thumbnail = null;

            try {
                if (MediaUtil.isImageType(attachment.getContentType()) && attachment.getDataUri() != null) {
                    thumbnailData = BitmapUtil.createScaledBytes(context, new DecryptableStreamUriLoader.DecryptableUri(attachment.getDataUri()), 100, 100, 500 * 1024);
                } else if (MediaUtil.isVideoType(attachment.getContentType()) && attachment.getThumbnailUri() != null) {
                    thumbnailData = BitmapUtil.createScaledBytes(context, new DecryptableStreamUriLoader.DecryptableUri(attachment.getThumbnailUri()), 100, 100, 500 * 1024);
                }

                if (thumbnailData != null) {
                    thumbnail = SignalServiceAttachment.newStreamBuilder()
                            .withContentType("image/jpeg")
                            .withWidth(thumbnailData.getWidth())
                            .withHeight(thumbnailData.getHeight())
                            .withLength(thumbnailData.getBitmap().length)
                            .withStream(new ByteArrayInputStream(thumbnailData.getBitmap()))
                            .build();
                }

                quoteAttachments.add(new SignalServiceDataMessage.Quote.QuotedAttachment(attachment.getContentType(),
                        attachment.getFileName(),
                        thumbnail));
            } catch (BitmapDecodingException e) {
                Log.w(TAG, e);
            }
        }

        return Optional.of(new SignalServiceDataMessage.Quote(quoteId, new SignalServiceAddress(quoteAuthor.serialize()), quoteBody, quoteAttachments));
    }

    List<SharedContact> getSharedContactsFor(OutgoingMediaMessage mediaMessage) {
        List<SharedContact> sharedContacts = new LinkedList<>();

        for (Contact contact : mediaMessage.getSharedContacts()) {
            SharedContact.Builder builder = ContactModelMapper.localToRemoteBuilder(contact);
            SharedContact.Avatar avatar = null;

            if (contact.getAvatar() != null && contact.getAvatar().getAttachment() != null) {
                avatar = SharedContact.Avatar.newBuilder().withAttachment(getAttachmentFor(contact.getAvatarAttachment()))
                        .withProfileFlag(contact.getAvatar().isProfile())
                        .build();
            }

            builder.setAvatar(avatar);
            sharedContacts.add(builder.build());
        }

        return sharedContacts;
    }

    protected void rotateSenderCertificateIfNecessary() throws IOException {
        try {
            byte[] certificateBytes = TextSecurePreferences.getUnidentifiedAccessCertificate(context);

            if (certificateBytes == null) {
                throw new InvalidCertificateException("No certificate was present.");
            }

            SenderCertificate certificate = new SenderCertificate(certificateBytes);

            if (System.currentTimeMillis() > (certificate.getExpiration() - CERTIFICATE_EXPIRATION_BUFFER)) {
                throw new InvalidCertificateException("Certificate is expired, or close to it. Expires on: " + certificate.getExpiration() + ", currently: " + System.currentTimeMillis());
            }

            Log.d(TAG, "Certificate is valid.");
        } catch (InvalidCertificateException e) {
            Log.w(TAG, "Certificate was invalid at send time. Fetching a new one.", e);
            RotateCertificateJob certificateJob = new RotateCertificateJob(context);
            MuzimaApplication.getInstance(context).injectDependencies(certificateJob);
            certificateJob.setContext(context);
            certificateJob.onRun();
        }
    }

    protected abstract void onPushSend() throws Exception;
}
