package com.muzima.messaging.sms;

import com.muzima.model.SignalRecipient;

public class OutgoingKeyExchangeMessage extends OutgoingTextMessage {

    public OutgoingKeyExchangeMessage(SignalRecipient recipient, String message) {
        super(recipient, message, -1);
    }

    private OutgoingKeyExchangeMessage(OutgoingKeyExchangeMessage base, String body) {
        super(base, body);
    }

    @Override
    public boolean isKeyExchange() {
        return true;
    }

    @Override
    public OutgoingTextMessage withBody(String body) {
        return new OutgoingKeyExchangeMessage(this, body);
    }
}
