package com.muzima.messaging.sms;

import com.muzima.model.SignalRecipient;

public class OutgoingIdentityVerifiedMessage extends OutgoingTextMessage {

    public OutgoingIdentityVerifiedMessage(SignalRecipient recipient) {
        this(recipient, "");
    }

    private OutgoingIdentityVerifiedMessage(SignalRecipient recipient, String body) {
        super(recipient, body, -1);
    }

    @Override
    public boolean isIdentityVerified() {
        return true;
    }

    @Override
    public OutgoingTextMessage withBody(String body) {
        return new OutgoingIdentityVerifiedMessage(getRecipient(), body);
    }
}
