package com.muzima.messaging.group;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.protobuf.ByteString;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.attachments.Attachment;
import com.muzima.messaging.attachments.UriAttachment;
import com.muzima.messaging.mms.OutgoingGroupMediaMessage;
import com.muzima.messaging.provider.MemoryBlobProvider;
import com.muzima.messaging.sms.MessageSender;
import com.muzima.messaging.sqlite.database.AttachmentDatabase;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.GroupDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.sqlite.database.ThreadDatabase;
import com.muzima.messaging.utils.GroupUtil;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.BitmapUtil;
import com.muzima.utils.MediaUtil;

import org.whispersystems.signalservice.api.util.InvalidNumberException;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class GroupManager {
    public static @NonNull GroupActionResult createGroup(@NonNull Context context,
                                  @NonNull Set<SignalRecipient> members,
                                  @Nullable Bitmap avatar,
                                  @Nullable String name,
                                  boolean mms) {
        final byte[] avatarBytes = BitmapUtil.toByteArray(avatar);
        final GroupDatabase groupDatabase = DatabaseFactory.getGroupDatabase(context);
        final String groupId = GroupUtil.getEncodedId(groupDatabase.allocateGroupId(), mms);
        final SignalRecipient groupRecipient = SignalRecipient.from(context, SignalAddress.fromSerialized(groupId), false);
        final Set<SignalAddress> memberAddresses = getMemberAddresses(members);

        memberAddresses.add(SignalAddress.fromSerialized(TextSecurePreferences.getLocalNumber(context)));
        groupDatabase.create(groupId, name, new LinkedList<>(memberAddresses), null, null);

        if (!mms) {
            groupDatabase.updateAvatar(groupId, avatarBytes);
            DatabaseFactory.getRecipientDatabase(context).setProfileSharing(groupRecipient, true);
            return sendGroupUpdate(context, groupId, memberAddresses, name, avatarBytes);
        } else {
            long threadId = DatabaseFactory.getThreadDatabase(context).getThreadIdFor(groupRecipient, ThreadDatabase.DistributionTypes.CONVERSATION);
            return new GroupActionResult(groupRecipient, threadId);
        }
    }

    public static GroupActionResult updateGroup(@NonNull Context context,
                                                @NonNull String groupId,
                                                @NonNull Set<SignalRecipient> members,
                                                @Nullable Bitmap avatar,
                                                @Nullable String name)
            throws InvalidNumberException {
        final GroupDatabase groupDatabase = DatabaseFactory.getGroupDatabase(context);
        final Set<SignalAddress> memberAddresses = getMemberAddresses(members);
        final byte[] avatarBytes = BitmapUtil.toByteArray(avatar);

        memberAddresses.add(SignalAddress.fromSerialized(TextSecurePreferences.getLocalNumber(context)));
        groupDatabase.updateMembers(groupId, new LinkedList<>(memberAddresses));
        groupDatabase.updateTitle(groupId, name);
        groupDatabase.updateAvatar(groupId, avatarBytes);

        if (!GroupUtil.isMmsGroup(groupId)) {
            return sendGroupUpdate(context, groupId, memberAddresses, name, avatarBytes);
        } else {
            SignalRecipient groupRecipient = SignalRecipient.from(context, SignalAddress.fromSerialized(groupId), true);
            long threadId = DatabaseFactory.getThreadDatabase(context).getThreadIdFor(groupRecipient);
            return new GroupActionResult(groupRecipient, threadId);
        }
    }

    private static GroupActionResult sendGroupUpdate(@NonNull Context context,
                                                     @NonNull String groupId,
                                                     @NonNull Set<SignalAddress> members,
                                                     @Nullable String groupName,
                                                     @Nullable byte[] avatar) {
        try {
            Attachment avatarAttachment = null;
            SignalAddress groupAddress = SignalAddress.fromSerialized(groupId);
            SignalRecipient groupRecipient = SignalRecipient.from(context, groupAddress, false);

            List<String> numbers = new LinkedList<>();

            for (SignalAddress member : members) {
                numbers.add(member.serialize());
            }

            SignalServiceProtos.GroupContext.Builder groupContextBuilder = SignalServiceProtos.GroupContext.newBuilder()
                    .setId(ByteString.copyFrom(GroupUtil.getDecodedId(groupId)))
                    .setType(SignalServiceProtos.GroupContext.Type.UPDATE)
                    .addAllMembers(numbers);
            if (groupName != null) groupContextBuilder.setName(groupName);
            SignalServiceProtos.GroupContext groupContext = groupContextBuilder.build();

            if (avatar != null) {
                Uri avatarUri = MemoryBlobProvider.getInstance().createSingleUseUri(avatar);
                avatarAttachment = new UriAttachment(avatarUri, MediaUtil.IMAGE_PNG, AttachmentDatabase.TRANSFER_PROGRESS_DONE, avatar.length, null, false, false, null);
            }

            OutgoingGroupMediaMessage outgoingMessage = new OutgoingGroupMediaMessage(groupRecipient, groupContext, avatarAttachment, System.currentTimeMillis(), 0, null, Collections.emptyList());
            long threadId = MessageSender.send(context, outgoingMessage, -1, false, null);

            return new GroupActionResult(groupRecipient, threadId);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static Set<SignalAddress> getMemberAddresses(Collection<SignalRecipient> recipients) {
        final Set<SignalAddress> results = new HashSet<>();
        for (SignalRecipient recipient : recipients) {
            results.add(recipient.getAddress());
        }

        return results;
    }

    public static class GroupActionResult {
        private SignalRecipient groupRecipient;
        private long threadId;

        public GroupActionResult(SignalRecipient groupRecipient, long threadId) {
            this.groupRecipient = groupRecipient;
            this.threadId = threadId;
        }

        public SignalRecipient getGroupRecipient() {
            return groupRecipient;
        }

        public long getThreadId() {
            return threadId;
        }
    }
}
