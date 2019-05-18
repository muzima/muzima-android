package com.muzima.messaging.mms;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.muzima.messaging.attachments.Attachment;
import com.muzima.messaging.contactshare.Contact;
import com.muzima.model.SignalRecipient;

import java.util.Collections;
import java.util.List;

public class OutgoingSecureMediaMessage extends OutgoingMediaMessage {
    public OutgoingSecureMediaMessage(SignalRecipient recipient, String body,
                                      List<Attachment> attachments,
                                      long sentTimeMillis,
                                      int distributionType,
                                      long expiresIn,
                                      @Nullable QuoteModel quote,
                                      @NonNull List<Contact> contacts) {
        super(recipient, body, attachments, sentTimeMillis, -1, expiresIn, distributionType, quote, contacts, Collections.emptyList(), Collections.emptyList());
    }

    public OutgoingSecureMediaMessage(OutgoingMediaMessage base) {
        super(base);
    }

    @Override
    public boolean isSecure() {
        return true;
    }
}
