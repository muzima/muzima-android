package com.muzima.messaging.sms;

import com.muzima.messaging.sqlite.database.SignalAddress;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;

public class IncomingJoinedMessage extends IncomingTextMessage {

    public IncomingJoinedMessage(SignalAddress sender) {
        super(sender, 1, System.currentTimeMillis(), null, Optional.<SignalServiceGroup>absent(), 0, false);
    }

    @Override
    public boolean isJoined() {
        return true;
    }

    @Override
    public boolean isSecureMessage() {
        return true;
    }
}
