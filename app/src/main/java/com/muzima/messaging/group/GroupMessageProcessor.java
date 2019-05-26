package com.muzima.messaging.group;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.protobuf.ByteString;
import com.muzima.MuzimaApplication;
import com.muzima.messaging.exceptions.MmsException;
import com.muzima.messaging.jobs.AvatarDownloadJob;
import com.muzima.messaging.jobs.PushGroupUpdateJob;
import com.muzima.messaging.mms.OutgoingGroupMediaMessage;
import com.muzima.messaging.sms.IncomingGroupMessage;
import com.muzima.messaging.sms.IncomingTextMessage;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.GroupDatabase;
import com.muzima.messaging.sqlite.database.GroupDatabase.GroupRecord;
import com.muzima.messaging.sqlite.database.MessagingDatabase.InsertResult;
import com.muzima.messaging.sqlite.database.MmsDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.sqlite.database.SmsDatabase;
import com.muzima.messaging.utils.GroupUtil;
import com.muzima.model.SignalRecipient;
import com.muzima.notifications.MessageNotifier;
import com.muzima.utils.Base64;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceContent;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class GroupMessageProcessor {
    private static final String TAG = GroupMessageProcessor.class.getSimpleName();

    public static @Nullable Long process(@NonNull Context context,
                                         @NonNull SignalServiceContent content,
                                         @NonNull SignalServiceDataMessage message,
                                         boolean outgoing) {
        if (!message.getGroupInfo().isPresent() || message.getGroupInfo().get().getGroupId() == null) {
            Log.w(TAG, "Received group message with no id! Ignoring...");
            return null;
        }

        GroupDatabase database = DatabaseFactory.getGroupDatabase(context);
        SignalServiceGroup group = message.getGroupInfo().get();
        String id = GroupUtil.getEncodedId(group.getGroupId(), false);
        Optional<GroupRecord> record = database.getGroup(id);

        if (record.isPresent() && group.getType() == SignalServiceGroup.Type.UPDATE) {
            return handleGroupUpdate(context, content, group, record.get(), outgoing);
        } else if (!record.isPresent() && group.getType() == SignalServiceGroup.Type.UPDATE) {
            return handleGroupCreate(context, content, group, outgoing);
        } else if (record.isPresent() && group.getType() == SignalServiceGroup.Type.QUIT) {
            return handleGroupLeave(context, content, group, record.get(), outgoing);
        } else if (record.isPresent() && group.getType() == SignalServiceGroup.Type.REQUEST_INFO) {
            return handleGroupInfoRequest(context, content, group, record.get());
        } else {
            Log.w(TAG, "Received unknown type, ignoring...");
            return null;
        }
    }

    private static @Nullable
    Long handleGroupCreate(@NonNull Context context,
                           @NonNull SignalServiceContent content,
                           @NonNull SignalServiceGroup group,
                           boolean outgoing) {
        GroupDatabase database = DatabaseFactory.getGroupDatabase(context);
        String id = GroupUtil.getEncodedId(group.getGroupId(), false);
        SignalServiceProtos.GroupContext.Builder builder = createGroupContext(group);
        builder.setType(SignalServiceProtos.GroupContext.Type.UPDATE);

        SignalServiceAttachment avatar = group.getAvatar().orNull();
        List<SignalAddress> members = group.getMembers().isPresent() ? new LinkedList<SignalAddress>() : null;

        if (group.getMembers().isPresent()) {
            for (String member : group.getMembers().get()) {
                members.add(SignalAddress.fromExternal(context, member));
            }
        }

        database.create(id, group.getName().orNull(), members,
                avatar != null && avatar.isPointer() ? avatar.asPointer() : null, null);

        return storeMessage(context, content, group, builder.build(), outgoing);
    }

    private static @Nullable
    Long handleGroupUpdate(@NonNull Context context,
                           @NonNull SignalServiceContent content,
                           @NonNull SignalServiceGroup group,
                           @NonNull GroupRecord groupRecord,
                           boolean outgoing) {

        GroupDatabase database = DatabaseFactory.getGroupDatabase(context);
        String id = GroupUtil.getEncodedId(group.getGroupId(), false);

        Set<SignalAddress> recordMembers = new HashSet<>(groupRecord.getMembers());
        Set<SignalAddress> messageMembers = new HashSet<>();

        for (String messageMember : group.getMembers().get()) {
            messageMembers.add(SignalAddress.fromExternal(context, messageMember));
        }

        Set<SignalAddress> addedMembers = new HashSet<>(messageMembers);
        addedMembers.removeAll(recordMembers);

        Set<SignalAddress> missingMembers = new HashSet<>(recordMembers);
        missingMembers.removeAll(messageMembers);

        SignalServiceProtos.GroupContext.Builder builder = createGroupContext(group);
        builder.setType(SignalServiceProtos.GroupContext.Type.UPDATE);

        if (addedMembers.size() > 0) {
            Set<SignalAddress> unionMembers = new HashSet<>(recordMembers);
            unionMembers.addAll(messageMembers);
            database.updateMembers(id, new LinkedList<>(unionMembers));

            builder.clearMembers();

            for (SignalAddress addedMember : addedMembers) {
                builder.addMembers(addedMember.serialize());
            }
        } else {
            builder.clearMembers();
        }

        if (missingMembers.size() > 0) {
            // TODO We should tell added and missing about each-other.
        }

        if (group.getName().isPresent() || group.getAvatar().isPresent()) {
            SignalServiceAttachment avatar = group.getAvatar().orNull();
            database.update(id, group.getName().orNull(), avatar != null ? avatar.asPointer() : null);
        }

        if (group.getName().isPresent() && group.getName().get().equals(groupRecord.getTitle())) {
            builder.clearName();
        }

        if (!groupRecord.isActive()) database.setActive(id, true);

        return storeMessage(context, content, group, builder.build(), outgoing);
    }

    private static Long handleGroupInfoRequest(@NonNull Context context,
                                               @NonNull SignalServiceContent content,
                                               @NonNull SignalServiceGroup group,
                                               @NonNull GroupRecord record) {
        if (record.getMembers().contains(SignalAddress.fromExternal(context, content.getSender()))) {
            MuzimaApplication.getInstance(context)
                    .getJobManager()
                    .add(new PushGroupUpdateJob(context, content.getSender(), group.getGroupId()));
        }

        return null;
    }

    private static Long handleGroupLeave(@NonNull Context context,
                                         @NonNull SignalServiceContent content,
                                         @NonNull SignalServiceGroup group,
                                         @NonNull GroupRecord record,
                                         boolean outgoing) {
        GroupDatabase database = DatabaseFactory.getGroupDatabase(context);
        String id = GroupUtil.getEncodedId(group.getGroupId(), false);
        List<SignalAddress> members = record.getMembers();

        SignalServiceProtos.GroupContext.Builder builder = createGroupContext(group);
        builder.setType(SignalServiceProtos.GroupContext.Type.QUIT);

        if (members.contains(SignalAddress.fromExternal(context, content.getSender()))) {
            database.remove(id, SignalAddress.fromExternal(context, content.getSender()));
            if (outgoing) database.setActive(id, false);

            return storeMessage(context, content, group, builder.build(), outgoing);
        }

        return null;
    }


    private static @Nullable
    Long storeMessage(@NonNull Context context,
                      @NonNull SignalServiceContent content,
                      @NonNull SignalServiceGroup group,
                      @NonNull SignalServiceProtos.GroupContext storage,
                      boolean outgoing) {
        if (group.getAvatar().isPresent()) {
            MuzimaApplication.getInstance(context).getJobManager()
                    .add(new AvatarDownloadJob(context, group.getGroupId()));
        }

        try {
            if (outgoing) {
                MmsDatabase mmsDatabase = DatabaseFactory.getMmsDatabase(context);
                SignalAddress addres = SignalAddress.fromExternal(context, GroupUtil.getEncodedId(group.getGroupId(), false));
                SignalRecipient recipient = SignalRecipient.from(context, addres, false);
                OutgoingGroupMediaMessage outgoingMessage = new OutgoingGroupMediaMessage(recipient, storage, null, content.getTimestamp(), 0, null, Collections.emptyList());
                long threadId = DatabaseFactory.getThreadDatabase(context).getThreadIdFor(recipient);
                long messageId = mmsDatabase.insertMessageOutbox(outgoingMessage, threadId, false, null);

                mmsDatabase.markAsSent(messageId, true);

                return threadId;
            } else {
                SmsDatabase smsDatabase = DatabaseFactory.getSmsDatabase(context);
                String body = Base64.encodeBytes(storage.toByteArray());
                IncomingTextMessage incoming = new IncomingTextMessage(SignalAddress.fromExternal(context, content.getSender()), content.getSenderDevice(), content.getTimestamp(), body, Optional.of(group), 0, content.isNeedsReceipt());
                IncomingGroupMessage groupMessage = new IncomingGroupMessage(incoming, storage, body);

                Optional<InsertResult> insertResult = smsDatabase.insertMessageInbox(groupMessage);

                if (insertResult.isPresent()) {
                    MessageNotifier.updateNotification(context, insertResult.get().getThreadId());
                    return insertResult.get().getThreadId();
                } else {
                    return null;
                }
            }
        } catch (MmsException e) {
            Log.w(TAG, e);
        }

        return null;
    }

    private static SignalServiceProtos.GroupContext.Builder createGroupContext(SignalServiceGroup group) {
        SignalServiceProtos.GroupContext.Builder builder = SignalServiceProtos.GroupContext.newBuilder();
        builder.setId(ByteString.copyFrom(group.getGroupId()));

        if (group.getAvatar().isPresent() && group.getAvatar().get().isPointer()) {
            builder.setAvatar(SignalServiceProtos.AttachmentPointer.newBuilder()
                    .setId(group.getAvatar().get().asPointer().getId())
                    .setKey(ByteString.copyFrom(group.getAvatar().get().asPointer().getKey()))
                    .setContentType(group.getAvatar().get().getContentType()));
        }

        if (group.getName().isPresent()) {
            builder.setName(group.getName().get());
        }

        if (group.getMembers().isPresent()) {
            builder.addAllMembers(group.getMembers().get());
        }

        return builder;
    }
}
