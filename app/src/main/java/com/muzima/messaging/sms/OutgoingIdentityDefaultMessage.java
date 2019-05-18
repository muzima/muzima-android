package com.muzima.messaging.sms;

import com.muzima.model.SignalRecipient;

public class OutgoingIdentityDefaultMessage extends OutgoingTextMessage {

    public OutgoingIdentityDefaultMessage(SignalRecipient recipient) {
        this(recipient, "");
    }

    private OutgoingIdentityDefaultMessage(SignalRecipient recipient, String body) {
        super(recipient, body, -1);
    }

    @Override
    public boolean isIdentityDefault() {
        return true;
    }

    public OutgoingTextMessage withBody(String body) {
        return new OutgoingIdentityDefaultMessage(getRecipient());
    }
}
