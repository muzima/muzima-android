package com.muzima.messaging.sms;

import org.whispersystems.signalservice.internal.push.SignalServiceProtos;

public class IncomingGroupMessage extends IncomingTextMessage {
    private final SignalServiceProtos.GroupContext groupContext;

    public IncomingGroupMessage(IncomingTextMessage base, SignalServiceProtos.GroupContext groupContext, String body) {
        super(base, body);
        this.groupContext = groupContext;
    }

    @Override
    public IncomingGroupMessage withMessageBody(String body) {
        return new IncomingGroupMessage(this, groupContext, body);
    }

    @Override
    public boolean isGroup() {
        return true;
    }

    public boolean isUpdate() {
        return groupContext.getType().getNumber() == SignalServiceProtos.GroupContext.Type.UPDATE_VALUE;
    }

    public boolean isQuit() {
        return groupContext.getType().getNumber() == SignalServiceProtos.GroupContext.Type.QUIT_VALUE;
    }
}
