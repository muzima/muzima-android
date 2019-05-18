package com.muzima.messaging.mms;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.muzima.messaging.attachments.Attachment;
import com.muzima.messaging.contactshare.Contact;
import com.muzima.messaging.sqlite.database.ThreadDatabase;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.Base64;

import org.whispersystems.signalservice.internal.push.SignalServiceProtos;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class OutgoingGroupMediaMessage extends OutgoingSecureMediaMessage {
    private final SignalServiceProtos.GroupContext group;

    public OutgoingGroupMediaMessage(@NonNull SignalRecipient recipient,
                                     @NonNull String encodedGroupContext,
                                     @NonNull List<Attachment> avatar,
                                     long sentTimeMillis,
                                     long expiresIn,
                                     @Nullable QuoteModel quote,
                                     @NonNull List<Contact> contacts)
            throws IOException {
        super(recipient, encodedGroupContext, avatar, sentTimeMillis,
                ThreadDatabase.DistributionTypes.CONVERSATION, expiresIn, quote, contacts);

        this.group = SignalServiceProtos.GroupContext.parseFrom(Base64.decode(encodedGroupContext));
    }

    public OutgoingGroupMediaMessage(@NonNull SignalRecipient recipient,
                                     @NonNull SignalServiceProtos.GroupContext group,
                                     @Nullable final Attachment avatar,
                                     long sentTimeMillis,
                                     long expireIn,
                                     @Nullable QuoteModel quote,
                                     @NonNull List<Contact> contacts) {
        super(recipient, Base64.encodeBytes(group.toByteArray()),
                new LinkedList<Attachment>() {{
                    if (avatar != null) add(avatar);
                }},
                System.currentTimeMillis(),
                ThreadDatabase.DistributionTypes.CONVERSATION, expireIn, quote, contacts);

        this.group = group;
    }

    @Override
    public boolean isGroup() {
        return true;
    }

    public boolean isGroupUpdate() {
        return group.getType().getNumber() == SignalServiceProtos.GroupContext.Type.UPDATE_VALUE;
    }

    public boolean isGroupQuit() {
        return group.getType().getNumber() == SignalServiceProtos.GroupContext.Type.QUIT_VALUE;
    }

    public SignalServiceProtos.GroupContext getGroupContext() {
        return group;
    }
}
