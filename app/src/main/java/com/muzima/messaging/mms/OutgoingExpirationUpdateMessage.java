package com.muzima.messaging.mms;

import com.muzima.messaging.attachments.Attachment;
import com.muzima.messaging.sqlite.database.ThreadDatabase;
import com.muzima.model.SignalRecipient;

import java.util.Collections;
import java.util.LinkedList;

public class OutgoingExpirationUpdateMessage extends OutgoingSecureMediaMessage {

    public OutgoingExpirationUpdateMessage(SignalRecipient recipient, long sentTimeMillis, long expiresIn) {
        super(recipient, "", new LinkedList<Attachment>(), sentTimeMillis,
                ThreadDatabase.DistributionTypes.CONVERSATION, expiresIn, null, Collections.emptyList());
    }

    @Override
    public boolean isExpirationUpdate() {
        return true;
    }

}
