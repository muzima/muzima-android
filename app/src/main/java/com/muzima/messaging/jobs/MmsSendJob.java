package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.android.mms.dom.smil.parser.SmilXmlSerializer;
import com.google.android.mms.ContentType;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.pdu_alt.CharacterSets;
import com.google.android.mms.pdu_alt.EncodedStringValue;
import com.google.android.mms.pdu_alt.PduBody;
import com.google.android.mms.pdu_alt.PduComposer;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduPart;
import com.google.android.mms.pdu_alt.SendConf;
import com.google.android.mms.pdu_alt.SendReq;
import com.google.android.mms.smil.SmilHelper;
import com.klinker.android.send_message.Utils;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.attachments.Attachment;
import com.muzima.messaging.exceptions.InsecureFallbackApprovalException;
import com.muzima.messaging.exceptions.MmsException;
import com.muzima.messaging.exceptions.UndeliverableMessageException;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.mms.CompatMmsConnection;
import com.muzima.messaging.mms.MediaConstraints;
import com.muzima.messaging.mms.MmsSendResult;
import com.muzima.messaging.mms.OutgoingMediaMessage;
import com.muzima.messaging.mms.PartAuthority;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.MmsDatabase;
import com.muzima.messaging.sqlite.database.NoSuchMessageException;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.sqlite.database.ThreadDatabase;
import com.muzima.messaging.utils.Util;
import com.muzima.model.SignalRecipient;
import com.muzima.notifications.MessageNotifier;
import com.muzima.utils.NumberUtil;

import org.whispersystems.libsignal.util.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class MmsSendJob extends SendJob {

    private static final long serialVersionUID = 0L;

    private static final String TAG = MmsSendJob.class.getSimpleName();

    private static final String KEY_MESSAGE_ID = "message_id";

    private long messageId;

    public MmsSendJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public MmsSendJob(Context context, long messageId) {
        super(context, JobParameters.newBuilder()
                .withGroupId("mms-operation")
                .withNetworkRequirement()
                .withRetryCount(15)
                .create());

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
    public void onSend() throws MmsException, NoSuchMessageException, IOException {
        MmsDatabase database = DatabaseFactory.getMmsDatabase(context);
        OutgoingMediaMessage message = database.getOutgoingMessage(messageId);

        if (database.isSent(messageId)) {
            Log.w(TAG, "Message " + messageId + " was already sent. Ignoring.");
            return;
        }

        try {
            Log.i(TAG, "Sending message: " + messageId);

            SendReq pdu = constructSendPdu(message);

            validateDestinations(message, pdu);

            final byte[] pduBytes = getPduBytes(pdu);
            final SendConf sendConf = new CompatMmsConnection(context).send(pduBytes, message.getSubscriptionId());
            final MmsSendResult result = getSendResult(sendConf, pdu);

            database.markAsSent(messageId, false);
            markAttachmentsUploaded(messageId, message.getAttachments());

            Log.i(TAG, "Sent message: " + messageId);
        } catch (UndeliverableMessageException | IOException e) {
            Log.w(TAG, e);
            database.markAsSentFailed(messageId);
            notifyMediaMessageDeliveryFailed(context, messageId);
        } catch (InsecureFallbackApprovalException e) {
            Log.w(TAG, e);
            database.markAsPendingInsecureSmsFallback(messageId);
            notifyMediaMessageDeliveryFailed(context, messageId);
        }
    }

    @Override
    public boolean onShouldRetry(Exception exception) {
        return false;
    }

    @Override
    public void onCanceled() {
        Log.i(TAG, "onCanceled() messageId: " + messageId);
        DatabaseFactory.getMmsDatabase(context).markAsSentFailed(messageId);
        notifyMediaMessageDeliveryFailed(context, messageId);
    }

    private byte[] getPduBytes(SendReq message)
            throws IOException, UndeliverableMessageException, InsecureFallbackApprovalException {
        byte[] pduBytes = new PduComposer(context, message).make();

        if (pduBytes == null) {
            throw new UndeliverableMessageException("PDU composition failed, null payload");
        }

        return pduBytes;
    }

    private MmsSendResult getSendResult(SendConf conf, SendReq message)
            throws UndeliverableMessageException {
        if (conf == null) {
            throw new UndeliverableMessageException("No M-Send.conf received in response to send.");
        } else if (conf.getResponseStatus() != PduHeaders.RESPONSE_STATUS_OK) {
            throw new UndeliverableMessageException("Got bad response: " + conf.getResponseStatus());
        } else if (isInconsistentResponse(message, conf)) {
            throw new UndeliverableMessageException("Mismatched response!");
        } else {
            return new MmsSendResult(conf.getMessageId(), conf.getResponseStatus());
        }
    }

    private boolean isInconsistentResponse(SendReq message, SendConf response) {
        Log.i(TAG, "Comparing: " + Hex.toString(message.getTransactionId()));
        Log.i(TAG, "With:      " + Hex.toString(response.getTransactionId()));
        return !Arrays.equals(message.getTransactionId(), response.getTransactionId());
    }

    private void validateDestinations(EncodedStringValue[] destinations) throws UndeliverableMessageException {
        if (destinations == null) return;

        for (EncodedStringValue destination : destinations) {
            if (destination == null || !NumberUtil.isValidSmsOrEmail(destination.getString())) {
                throw new UndeliverableMessageException("Invalid destination: " +
                        (destination == null ? null : destination.getString()));
            }
        }
    }

    private void validateDestinations(OutgoingMediaMessage media, SendReq message) throws UndeliverableMessageException {
        validateDestinations(message.getTo());
        validateDestinations(message.getCc());
        validateDestinations(message.getBcc());

        if (message.getTo() == null && message.getCc() == null && message.getBcc() == null) {
            throw new UndeliverableMessageException("No to, cc, or bcc specified!");
        }

        if (media.isSecure()) {
            throw new UndeliverableMessageException("Attempt to send encrypted MMS?");
        }
    }

    private SendReq constructSendPdu(OutgoingMediaMessage message)
            throws UndeliverableMessageException {
        SendReq req = new SendReq();
        String lineNumber = getMyNumber(context);
        SignalAddress destination = message.getRecipient().getAddress();
        MediaConstraints mediaConstraints = MediaConstraints.getMmsMediaConstraints(message.getSubscriptionId());
        List<Attachment> scaledAttachments = scaleAndStripExifFromAttachments(mediaConstraints, message.getAttachments());

        if (!TextUtils.isEmpty(lineNumber)) {
            req.setFrom(new EncodedStringValue(lineNumber));
        } else {
            req.setFrom(new EncodedStringValue(TextSecurePreferences.getLocalNumber(context)));
        }

        if (destination.isMmsGroup()) {
            List<SignalRecipient> members = DatabaseFactory.getGroupDatabase(context).getGroupMembers(destination.toGroupString(), false);

            for (SignalRecipient member : members) {
                if (message.getDistributionType() == ThreadDatabase.DistributionTypes.BROADCAST) {
                    req.addBcc(new EncodedStringValue(member.getAddress().serialize()));
                } else {
                    req.addTo(new EncodedStringValue(member.getAddress().serialize()));
                }
            }
        } else {
            req.addTo(new EncodedStringValue(destination.serialize()));
        }

        req.setDate(System.currentTimeMillis() / 1000);

        PduBody body = new PduBody();
        int size = 0;

        if (!TextUtils.isEmpty(message.getBody())) {
            PduPart part = new PduPart();
            String name = String.valueOf(System.currentTimeMillis());
            part.setData(Util.toUtf8Bytes(message.getBody()));
            part.setCharset(CharacterSets.UTF_8);
            part.setContentType(ContentType.TEXT_PLAIN.getBytes());
            part.setContentId(name.getBytes());
            part.setContentLocation((name + ".txt").getBytes());
            part.setName((name + ".txt").getBytes());

            body.addPart(part);
            size += getPartSize(part);
        }

        for (Attachment attachment : scaledAttachments) {
            try {
                if (attachment.getDataUri() == null)
                    throw new IOException("Assertion failed, attachment for outgoing MMS has no data!");

                String fileName = attachment.getFileName();
                PduPart part = new PduPart();

                if (fileName == null) {
                    fileName = String.valueOf(Math.abs(Util.getSecureRandom().nextLong()));
                    String fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(attachment.getContentType());

                    if (fileExtension != null) fileName = fileName + "." + fileExtension;
                }

                if (attachment.getContentType().startsWith("text")) {
                    part.setCharset(CharacterSets.UTF_8);
                }

                part.setContentType(attachment.getContentType().getBytes());
                part.setContentLocation(fileName.getBytes());
                part.setName(fileName.getBytes());

                int index = fileName.lastIndexOf(".");
                String contentId = (index == -1) ? fileName : fileName.substring(0, index);
                part.setContentId(contentId.getBytes());
                part.setData(Util.readFully(PartAuthority.getAttachmentStream(context, attachment.getDataUri())));

                body.addPart(part);
                size += getPartSize(part);
            } catch (IOException e) {
                Log.w(TAG, e);
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmilXmlSerializer.serialize(SmilHelper.createSmilDocument(body), out);
        PduPart smilPart = new PduPart();
        smilPart.setContentId("smil".getBytes());
        smilPart.setContentLocation("smil.xml".getBytes());
        smilPart.setContentType(ContentType.APP_SMIL.getBytes());
        smilPart.setData(out.toByteArray());
        body.addPart(0, smilPart);

        req.setBody(body);
        req.setMessageSize(size);
        req.setMessageClass(PduHeaders.MESSAGE_CLASS_PERSONAL_STR.getBytes());
        req.setExpiry(7 * 24 * 60 * 60);

        try {
            req.setPriority(PduHeaders.PRIORITY_NORMAL);
            req.setDeliveryReport(PduHeaders.VALUE_NO);
            req.setReadReport(PduHeaders.VALUE_NO);
        } catch (InvalidHeaderValueException e) {
        }

        return req;
    }

    private long getPartSize(PduPart part) {
        return part.getName().length + part.getContentLocation().length +
                part.getContentType().length + part.getData().length +
                part.getContentId().length;
    }

    private void notifyMediaMessageDeliveryFailed(Context context, long messageId) {
        long threadId = DatabaseFactory.getMmsDatabase(context).getThreadIdForMessage(messageId);
        SignalRecipient recipient = DatabaseFactory.getThreadDatabase(context).getRecipientForThreadId(threadId);

        if (recipient != null) {
            MessageNotifier.notifyMessageDeliveryFailed(context, recipient, threadId);
        }
    }

    private String getMyNumber(Context context) throws UndeliverableMessageException {
        try {
            return Utils.getMyPhoneNumber(context);
        } catch (SecurityException e) {
            throw new UndeliverableMessageException(e);
        }
    }
}
