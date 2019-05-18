package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.muzima.MuzimaApplication;
import com.muzima.messaging.attachments.Attachment;
import com.muzima.messaging.crypto.UnidentifiedAccessUtil;
import com.muzima.messaging.exceptions.MmsException;
import com.muzima.messaging.exceptions.RecipientFormattingException;
import com.muzima.messaging.exceptions.RetryLaterException;
import com.muzima.messaging.exceptions.UndeliverableMessageException;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.jobmanager.dependencies.InjectableType;
import com.muzima.messaging.mms.MediaConstraints;
import com.muzima.messaging.mms.OutgoingGroupMediaMessage;
import com.muzima.messaging.mms.OutgoingMediaMessage;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.GroupReceiptDatabase.GroupReceiptInfo;
import com.muzima.messaging.sqlite.database.MmsDatabase;
import com.muzima.messaging.sqlite.database.NoSuchMessageException;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.sqlite.database.documents.IdentityKeyMismatch;
import com.muzima.messaging.sqlite.database.documents.NetworkFailure;
import com.muzima.messaging.utils.GroupUtil;
import com.muzima.model.SignalRecipient;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UnidentifiedAccessPair;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SendMessageResult;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;
import org.whispersystems.signalservice.api.messages.shared.SharedContact;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.util.InvalidNumberException;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class PushGroupSendJob extends PushSendJob implements InjectableType {

    private static final long serialVersionUID = 1L;

    private static final String TAG = PushGroupSendJob.class.getSimpleName();

    @Inject
    transient SignalServiceMessageSender messageSender;

    private static final String KEY_MESSAGE_ID = "message_id";
    private static final String KEY_FILTER_ADDRESS = "filter_address";

    private long messageId;
    private long filterRecipientId; // Deprecated
    private String filterAddress;

    public PushGroupSendJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public PushGroupSendJob(Context context, long messageId, @NonNull SignalAddress destination, @Nullable SignalAddress filterAddress) {
        super(context, JobParameters.newBuilder()
                .withGroupId(destination.toGroupString())
                .withNetworkRequirement()
                .withRetryDuration(TimeUnit.DAYS.toMillis(1))
                .create());

        this.messageId = messageId;
        this.filterAddress = filterAddress == null ? null : filterAddress.toPhoneString();
        this.filterRecipientId = -1;
    }

    @Override
    protected void initialize(@NonNull SafeData data) {
        messageId = data.getLong(KEY_MESSAGE_ID);
        filterAddress = data.getString(KEY_FILTER_ADDRESS);
    }

    @Override
    protected @NonNull
    Data serialize(@NonNull Data.Builder dataBuilder) {
        return dataBuilder.putLong(KEY_MESSAGE_ID, messageId)
                .putString(KEY_FILTER_ADDRESS, filterAddress)
                .build();
    }

    @Override
    protected void onAdded() {
        DatabaseFactory.getMmsDatabase(context).markAsSending(messageId);
    }

    @Override
    public void onPushSend()
            throws IOException, MmsException, NoSuchMessageException, RetryLaterException {
        MmsDatabase database = DatabaseFactory.getMmsDatabase(context);
        OutgoingMediaMessage message = database.getOutgoingMessage(messageId);
        List<NetworkFailure> existingNetworkFailures = message.getNetworkFailures();
        List<IdentityKeyMismatch> existingIdentityMismatches = message.getIdentityKeyMismatches();

        if (database.isSent(messageId)) {
            log(TAG, "Message " + messageId + " was already sent. Ignoring.");
            return;
        }

        try {
            log(TAG, "Sending message: " + messageId);

            List<SignalAddress> target;

            if (filterAddress != null)
                target = Collections.singletonList(SignalAddress.fromSerialized(filterAddress));
            else if (!existingNetworkFailures.isEmpty())
                target = Stream.of(existingNetworkFailures).map(NetworkFailure::getAddress).toList();
            else
                target = getGroupMessageRecipients(message.getRecipient().getAddress().toGroupString(), messageId);

            List<SendMessageResult> results = deliver(message, target);
            List<NetworkFailure> networkFailures = Stream.of(results).filter(SendMessageResult::isNetworkFailure).map(result -> new NetworkFailure(SignalAddress.fromSerialized(result.getAddress().getNumber()))).toList();
            List<IdentityKeyMismatch> identityMismatches = Stream.of(results).filter(result -> result.getIdentityFailure() != null).map(result -> new IdentityKeyMismatch(SignalAddress.fromSerialized(result.getAddress().getNumber()), result.getIdentityFailure().getIdentityKey())).toList();
            Set<SignalAddress> successAddresses = Stream.of(results).filter(result -> result.getSuccess() != null).map(result -> SignalAddress.fromSerialized(result.getAddress().getNumber())).collect(Collectors.toSet());
            List<NetworkFailure> resolvedNetworkFailures = Stream.of(existingNetworkFailures).filter(failure -> successAddresses.contains(failure.getAddress())).toList();
            List<IdentityKeyMismatch> resolvedIdentityFailures = Stream.of(existingIdentityMismatches).filter(failure -> successAddresses.contains(failure.getAddress())).toList();
            List<SendMessageResult> successes = Stream.of(results).filter(result -> result.getSuccess() != null).toList();

            for (NetworkFailure resolvedFailure : resolvedNetworkFailures) {
                database.removeFailure(messageId, resolvedFailure);
                existingNetworkFailures.remove(resolvedFailure);
            }

            for (IdentityKeyMismatch resolvedIdentity : resolvedIdentityFailures) {
                database.removeMismatchedIdentity(messageId, resolvedIdentity.getAddress(), resolvedIdentity.getIdentityKey());
                existingIdentityMismatches.remove(resolvedIdentity);
            }

            if (!networkFailures.isEmpty()) {
                database.addFailures(messageId, networkFailures);
            }

            for (IdentityKeyMismatch mismatch : identityMismatches) {
                database.addMismatchedIdentity(messageId, mismatch.getAddress(), mismatch.getIdentityKey());
            }

            for (SendMessageResult success : successes) {
                DatabaseFactory.getGroupReceiptDatabase(context).setUnidentified(SignalAddress.fromSerialized(success.getAddress().getNumber()),
                        messageId,
                        success.getSuccess().isUnidentified());
            }

            if (existingNetworkFailures.isEmpty() && networkFailures.isEmpty() && identityMismatches.isEmpty() && existingIdentityMismatches.isEmpty()) {
                database.markAsSent(messageId, true);

                markAttachmentsUploaded(messageId, message.getAttachments());

                if (message.getExpiresIn() > 0 && !message.isExpirationUpdate()) {
                    database.markExpireStarted(messageId);
                    MuzimaApplication.getInstance(context)
                            .getExpiringMessageManager()
                            .scheduleDeletion(messageId, true, message.getExpiresIn());
                }
            } else if (!networkFailures.isEmpty()) {
                throw new RetryLaterException();
            } else if (!identityMismatches.isEmpty()) {
                database.markAsSentFailed(messageId);
                notifyMediaMessageDeliveryFailed(context, messageId);
            }

        } catch (InvalidNumberException | RecipientFormattingException | UndeliverableMessageException e) {
            warn(TAG, e);
            database.markAsSentFailed(messageId);
            notifyMediaMessageDeliveryFailed(context, messageId);
        } catch (UntrustedIdentityException e) {
            warn(TAG, e);
            database.markAsSentFailed(messageId);
            notifyMediaMessageDeliveryFailed(context, messageId);
        }
    }

    @Override
    public boolean onShouldRetry(Exception exception) {
        if (exception instanceof IOException) return true;
        if (exception instanceof RetryLaterException) return true;
        return false;
    }

    @Override
    public void onCanceled() {
        DatabaseFactory.getMmsDatabase(context).markAsSentFailed(messageId);
    }

    private List<SendMessageResult> deliver(OutgoingMediaMessage message, @NonNull List<SignalAddress> destinations)
            throws IOException, RecipientFormattingException, InvalidNumberException,
            UndeliverableMessageException, UntrustedIdentityException {
        rotateSenderCertificateIfNecessary();

        String groupId = message.getRecipient().getAddress().toGroupString();
        Optional<byte[]> profileKey = getProfileKey(message.getRecipient());
        MediaConstraints mediaConstraints = MediaConstraints.getPushMediaConstraints();
        List<Attachment> scaledAttachments = scaleAndStripExifFromAttachments(mediaConstraints, message.getAttachments());
        List<SignalServiceAttachment> attachmentStreams = getAttachmentsFor(scaledAttachments);
        Optional<SignalServiceDataMessage.Quote> quote = getQuoteFor(message);
        List<SharedContact> sharedContacts = getSharedContactsFor(message);
        List<SignalServiceAddress> addresses = Stream.of(destinations).map(this::getPushAddress).toList();

        List<Optional<UnidentifiedAccessPair>> unidentifiedAccess = Stream.of(addresses)
                .map(address -> SignalAddress.fromSerialized(address.getNumber()))
                .map(address -> SignalRecipient.from(context, address, false))
                .map(recipient -> UnidentifiedAccessUtil.getAccessFor(context, recipient))
                .toList();

        if (message.isGroup()) {
            OutgoingGroupMediaMessage groupMessage = (OutgoingGroupMediaMessage) message;
            SignalServiceProtos.GroupContext groupContext = groupMessage.getGroupContext();
            SignalServiceAttachment avatar = attachmentStreams.isEmpty() ? null : attachmentStreams.get(0);
            SignalServiceGroup.Type type = groupMessage.isGroupQuit() ? SignalServiceGroup.Type.QUIT : SignalServiceGroup.Type.UPDATE;
            SignalServiceGroup group = new SignalServiceGroup(type, GroupUtil.getDecodedId(groupId), groupContext.getName(), groupContext.getMembersList(), avatar);
            SignalServiceDataMessage groupDataMessage = SignalServiceDataMessage.newBuilder()
                    .withTimestamp(message.getSentTimeMillis())
                    .withExpiration(message.getRecipient().getExpireMessages())
                    .asGroupMessage(group)
                    .build();

            return messageSender.sendMessage(addresses, unidentifiedAccess, groupDataMessage);
        } else {
            SignalServiceGroup group = new SignalServiceGroup(GroupUtil.getDecodedId(groupId));
            SignalServiceDataMessage groupMessage = SignalServiceDataMessage.newBuilder()
                    .withTimestamp(message.getSentTimeMillis())
                    .asGroupMessage(group)
                    .withAttachments(attachmentStreams)
                    .withBody(message.getBody())
                    .withExpiration((int) (message.getExpiresIn() / 1000))
                    .asExpirationUpdate(message.isExpirationUpdate())
                    .withProfileKey(profileKey.orNull())
                    .withQuote(quote.orNull())
                    .withSharedContacts(sharedContacts)
                    .build();

            return messageSender.sendMessage(addresses, unidentifiedAccess, groupMessage);
        }
    }

    private @NonNull
    List<SignalAddress> getGroupMessageRecipients(String groupId, long messageId) {
        List<GroupReceiptInfo> destinations = DatabaseFactory.getGroupReceiptDatabase(context).getGroupReceiptInfo(messageId);
        if (!destinations.isEmpty())
            return Stream.of(destinations).map(GroupReceiptInfo::getAddress).toList();

        List<SignalRecipient> members = DatabaseFactory.getGroupDatabase(context).getGroupMembers(groupId, false);
        return Stream.of(members).map(SignalRecipient::getAddress).toList();
    }
}
