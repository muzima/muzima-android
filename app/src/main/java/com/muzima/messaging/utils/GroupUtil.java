package com.muzima.messaging.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.google.protobuf.ByteString;
import com.muzima.R;
import com.muzima.messaging.RecipientModifiedListener;
import com.muzima.messaging.mms.OutgoingGroupMediaMessage;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.GroupDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.Base64;

import org.whispersystems.libsignal.util.Hex;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class GroupUtil {
    private static final String ENCODED_SIGNAL_GROUP_PREFIX = "__textsecure_group__!";
    private static final String ENCODED_MMS_GROUP_PREFIX    = "__signal_mms_group__!";
    private static final String TAG                         = GroupUtil.class.getSimpleName();

    public static String getEncodedId(byte[] groupId, boolean mms) {
        return (mms ? ENCODED_MMS_GROUP_PREFIX  : ENCODED_SIGNAL_GROUP_PREFIX) + Hex.toStringCondensed(groupId);
    }

    public static byte[] getDecodedId(String groupId) throws IOException {
        if (!isEncodedGroup(groupId)) {
            throw new IOException("Invalid encoding");
        }

        return Hex.fromStringCondensed(groupId.split("!", 2)[1]);
    }

    public static boolean isEncodedGroup(@NonNull String groupId) {
        return groupId.startsWith(ENCODED_SIGNAL_GROUP_PREFIX) || groupId.startsWith(ENCODED_MMS_GROUP_PREFIX);
    }

    public static boolean isMmsGroup(@NonNull String groupId) {
        return groupId.startsWith(ENCODED_MMS_GROUP_PREFIX);
    }

    @WorkerThread
    public static Optional<OutgoingGroupMediaMessage> createGroupLeaveMessage(@NonNull Context context, @NonNull SignalRecipient groupRecipient) {
        String        encodedGroupId = groupRecipient.getAddress().toGroupString();
        GroupDatabase groupDatabase  = DatabaseFactory.getGroupDatabase(context);

        if (!groupDatabase.isActive(encodedGroupId)) {
            Log.w(TAG, "Group has already been left.");
            return Optional.absent();
        }

        ByteString decodedGroupId;
        try {
            decodedGroupId = ByteString.copyFrom(getDecodedId(encodedGroupId));
        } catch (IOException e) {
            Log.w(TAG, "Failed to decode group ID.", e);
            return Optional.absent();
        }

        SignalServiceProtos.GroupContext groupContext = SignalServiceProtos.GroupContext.newBuilder()
                .setId(decodedGroupId)
                .setType(SignalServiceProtos.GroupContext.Type.QUIT)
                .build();

        return Optional.of(new OutgoingGroupMediaMessage(groupRecipient, groupContext, null, System.currentTimeMillis(), 0, null, Collections.emptyList()));
    }


    public static @NonNull GroupDescription getDescription(@NonNull Context context, @Nullable String encodedGroup) {
        if (encodedGroup == null) {
            return new GroupDescription(context, null);
        }

        try {
            SignalServiceProtos.GroupContext groupContext = SignalServiceProtos.GroupContext.parseFrom(Base64.decode(encodedGroup));
            return new GroupDescription(context, groupContext);
        } catch (IOException e) {
            Log.w(TAG, e);
            return new GroupDescription(context, null);
        }
    }

    public static class GroupDescription {

        @NonNull  private final Context         context;
        @Nullable private final SignalServiceProtos.GroupContext groupContext;
        @Nullable private final List<SignalRecipient> members;

        public GroupDescription(@NonNull Context context, @Nullable SignalServiceProtos.GroupContext groupContext) {
            this.context      = context.getApplicationContext();
            this.groupContext = groupContext;

            if (groupContext == null || groupContext.getMembersList().isEmpty()) {
                this.members = null;
            } else {
                this.members = new LinkedList<>();

                for (String member : groupContext.getMembersList()) {
                    this.members.add(SignalRecipient.from(context, SignalAddress.fromExternal(context, member), true));
                }
            }
        }

        public String toString(SignalRecipient sender) {
            StringBuilder description = new StringBuilder();
            description.append(context.getString(R.string.message_record_s_updated_group, sender.toShortString()));

            if (groupContext == null) {
                return description.toString();
            }

            String title = groupContext.getName();

            if (members != null) {
                description.append("\n");
                description.append(context.getResources().getQuantityString(R.plurals.group_util_joined_the_group,
                        members.size(), toString(members)));
            }

            if (title != null && !title.trim().isEmpty()) {
                if (members != null) description.append(" ");
                else                 description.append("\n");
                description.append(context.getString(R.string.group_util_group_name_is_now, title));
            }

            return description.toString();
        }

        public void addListener(RecipientModifiedListener listener) {
            if (this.members != null) {
                for (SignalRecipient member : this.members) {
                    member.addListener(listener);
                }
            }
        }

        private String toString(List<SignalRecipient> recipients) {
            String result = "";

            for (int i=0;i<recipients.size();i++) {
                result += recipients.get(i).toShortString();

                if (i != recipients.size() -1 )
                    result += ", ";
            }

            return result;
        }
    }
}
