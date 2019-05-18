package com.muzima.messaging.mms;

import com.muzima.messaging.attachments.Attachment;
import com.muzima.messaging.attachments.PointerAttachment;
import com.muzima.messaging.contactshare.Contact;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.utils.GroupUtil;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class IncomingMediaMessage {
    private final SignalAddress from;
    private final SignalAddress groupId;
    private final String body;
    private final boolean push;
    private final long sentTimeMillis;
    private final int subscriptionId;
    private final long expiresIn;
    private final boolean expirationUpdate;
    private final QuoteModel quote;
    private final boolean unidentified;

    private final List<Attachment> attachments = new LinkedList<>();
    private final List<Contact> sharedContacts = new LinkedList<>();

    public IncomingMediaMessage(SignalAddress from,
                                Optional<SignalAddress> groupId,
                                String body,
                                long sentTimeMillis,
                                List<Attachment> attachments,
                                int subscriptionId,
                                long expiresIn,
                                boolean expirationUpdate,
                                boolean unidentified) {
        this.from = from;
        this.groupId = groupId.orNull();
        this.sentTimeMillis = sentTimeMillis;
        this.body = body;
        this.push = false;
        this.subscriptionId = subscriptionId;
        this.expiresIn = expiresIn;
        this.expirationUpdate = expirationUpdate;
        this.quote = null;
        this.unidentified = unidentified;

        this.attachments.addAll(attachments);
    }

    public IncomingMediaMessage(SignalAddress from,
                                long sentTimeMillis,
                                int subscriptionId,
                                long expiresIn,
                                boolean expirationUpdate,
                                boolean unidentified,
                                Optional<String> body,
                                Optional<SignalServiceGroup> group,
                                Optional<List<SignalServiceAttachment>> attachments,
                                Optional<QuoteModel> quote,
                                Optional<List<Contact>> sharedContacts) {
        this.push = true;
        this.from = from;
        this.sentTimeMillis = sentTimeMillis;
        this.body = body.orNull();
        this.subscriptionId = subscriptionId;
        this.expiresIn = expiresIn;
        this.expirationUpdate = expirationUpdate;
        this.quote = quote.orNull();
        this.unidentified = unidentified;

        if (group.isPresent())
            this.groupId = SignalAddress.fromSerialized(GroupUtil.getEncodedId(group.get().getGroupId(), false));
        else this.groupId = null;

        this.attachments.addAll(PointerAttachment.forPointers(attachments));
        this.sharedContacts.addAll(sharedContacts.or(Collections.emptyList()));
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public String getBody() {
        return body;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public SignalAddress getFrom() {
        return from;
    }

    public SignalAddress getGroupId() {
        return groupId;
    }

    public boolean isPushMessage() {
        return push;
    }

    public boolean isExpirationUpdate() {
        return expirationUpdate;
    }

    public long getSentTimeMillis() {
        return sentTimeMillis;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public boolean isGroupMessage() {
        return groupId != null;
    }

    public QuoteModel getQuote() {
        return quote;
    }

    public List<Contact> getSharedContacts() {
        return sharedContacts;
    }

    public boolean isUnidentified() {
        return unidentified;
    }
}
