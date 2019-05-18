package com.muzima.messaging;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.muzima.R;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.GroupDatabase;
import com.muzima.messaging.sqlite.database.GroupDatabase.GroupRecord;
import com.muzima.messaging.sqlite.database.RecipientDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.utils.Util;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.ListenableFutureTask;
import com.muzima.utils.MaterialColor;
import com.muzima.utils.SoftHashMap;

import org.whispersystems.libsignal.util.guava.Optional;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class RecipientProvider {
    @SuppressWarnings("unused")
    private static final String TAG = RecipientProvider.class.getSimpleName();

    private static final RecipientCache recipientCache = new RecipientCache();
    private static final ExecutorService asyncRecipientResolver = Util.newSingleThreadedLifoExecutor();

    private static final Map<String, RecipientDetails> STATIC_DETAILS = new HashMap<String, RecipientDetails>() {{
        put("262966", new RecipientDetails("Amazon", null, false, null, null));
    }};

    @NonNull
    public SignalRecipient getRecipient(@NonNull Context context, @NonNull SignalAddress address, @NonNull Optional<RecipientDatabase.RecipientSettings> settings, @NonNull Optional<GroupRecord> groupRecord, boolean asynchronous) {
        SignalRecipient cachedRecipient = recipientCache.get(address);

        if (cachedRecipient != null && (asynchronous || !cachedRecipient.isResolving()) && ((!groupRecord.isPresent() && !settings.isPresent()) || !cachedRecipient.isResolving() || cachedRecipient.getName() != null)) {
            return cachedRecipient;
        }

        Optional<RecipientDetails> prefetchedRecipientDetails = createPrefetchedRecipientDetails(context, address, settings, groupRecord);

        if (asynchronous) {
            cachedRecipient = new SignalRecipient(address, cachedRecipient, prefetchedRecipientDetails, getRecipientDetailsAsync(context, address, settings, groupRecord));
        } else {
            cachedRecipient = new SignalRecipient(address, getRecipientDetailsSync(context, address, settings, groupRecord, false));
        }

        recipientCache.set(address, cachedRecipient);
        return cachedRecipient;
    }

    @NonNull
    public Optional<SignalRecipient> getCached(@NonNull SignalAddress address) {
        return Optional.fromNullable(recipientCache.get(address));
    }

    private @NonNull
    Optional<RecipientDetails> createPrefetchedRecipientDetails(@NonNull Context context, @NonNull SignalAddress address,
                                                                @NonNull Optional<RecipientDatabase.RecipientSettings> settings,
                                                                @NonNull Optional<GroupRecord> groupRecord) {
        if (address.isGroup() && settings.isPresent() && groupRecord.isPresent()) {
            return Optional.of(getGroupRecipientDetails(context, address, groupRecord, settings, true));
        } else if (!address.isGroup() && settings.isPresent()) {
            return Optional.of(new RecipientDetails(null, null, !TextUtils.isEmpty(settings.get().getSystemDisplayName()), settings.get(), null));
        }

        return Optional.absent();
    }

    private @NonNull
    ListenableFutureTask<RecipientDetails> getRecipientDetailsAsync(final Context context, final @NonNull SignalAddress address, final @NonNull Optional<RecipientDatabase.RecipientSettings> settings, final @NonNull Optional<GroupRecord> groupRecord) {
        Callable<RecipientDetails> task = () -> getRecipientDetailsSync(context, address, settings, groupRecord, true);

        ListenableFutureTask<RecipientDetails> future = new ListenableFutureTask<>(task);
        asyncRecipientResolver.submit(future);
        return future;
    }

    private @NonNull
    RecipientDetails getRecipientDetailsSync(Context context, @NonNull SignalAddress address, Optional<RecipientDatabase.RecipientSettings> settings, Optional<GroupRecord> groupRecord, boolean nestedAsynchronous) {
        if (address.isGroup())
            return getGroupRecipientDetails(context, address, groupRecord, settings, nestedAsynchronous);
        else return getIndividualRecipientDetails(context, address, settings);
    }

    private @NonNull
    RecipientDetails getIndividualRecipientDetails(Context context, @NonNull SignalAddress address, Optional<RecipientDatabase.RecipientSettings> settings) {
        if (!settings.isPresent()) {
            settings = DatabaseFactory.getRecipientDatabase(context).getRecipientSettings(address);
        }

        if (!settings.isPresent() && STATIC_DETAILS.containsKey(address.serialize())) {
            return STATIC_DETAILS.get(address.serialize());
        } else {
            boolean systemContact = settings.isPresent() && !TextUtils.isEmpty(settings.get().getSystemDisplayName());
            return new RecipientDetails(null, null, systemContact, settings.orNull(), null);
        }
    }

    private @NonNull
    RecipientDetails getGroupRecipientDetails(Context context, SignalAddress groupId, Optional<GroupRecord> groupRecord, Optional<RecipientDatabase.RecipientSettings> settings, boolean asynchronous) {

        if (!groupRecord.isPresent()) {
            groupRecord = DatabaseFactory.getGroupDatabase(context).getGroup(groupId.toGroupString());
        }

        if (!settings.isPresent()) {
            settings = DatabaseFactory.getRecipientDatabase(context).getRecipientSettings(groupId);
        }

        if (groupRecord.isPresent()) {
            String title = groupRecord.get().getTitle();
            List<SignalAddress> memberAddresses = groupRecord.get().getMembers();
            List<SignalRecipient> members = new LinkedList<>();
            Long avatarId = null;

            for (SignalAddress memberAddress : memberAddresses) {
                members.add(getRecipient(context, memberAddress, Optional.absent(), Optional.absent(), asynchronous));
            }

            if (!groupId.isMmsGroup() && title == null) {
                title = context.getString(R.string.recipient_provider_unnamed_group);
            }

            if (groupRecord.get().getAvatar() != null && groupRecord.get().getAvatar().length > 0) {
                avatarId = groupRecord.get().getAvatarId();
            }

            return new RecipientDetails(title, avatarId, false, settings.orNull(), members);
        }

        return new RecipientDetails(context.getString(R.string.recipient_provider_unnamed_group), null, false, settings.orNull(), null);
    }

    public static class RecipientDetails {
        @Nullable
        public final String name;
        @Nullable
        public final String customLabel;
        @Nullable
        public final Uri systemContactPhoto;
        @Nullable
        public final Uri contactUri;
        @Nullable
        public final Long groupAvatarId;
        @Nullable
        public final MaterialColor color;
        @Nullable
        public final Uri messageRingtone;
        @Nullable
        public final Uri callRingtone;
        public final long mutedUntil;
        @Nullable
        public final RecipientDatabase.VibrateState messageVibrateState;
        @Nullable
        public final RecipientDatabase.VibrateState callVibrateState;
        public final boolean blocked;
        public final int expireMessages;
        @NonNull
        public final List<SignalRecipient> participants;
        @Nullable
        public final String profileName;
        public final boolean seenInviteReminder;
        public final Optional<Integer> defaultSubscriptionId;
        @NonNull
        public final RecipientDatabase.RegisteredState registered;
        @Nullable
        public final byte[] profileKey;
        @Nullable
        public final String profileAvatar;
        public final boolean profileSharing;
        public final boolean systemContact;
        @Nullable
        public final String notificationChannel;
        @NonNull
        public final RecipientDatabase.UnidentifiedAccessMode unidentifiedAccessMode;

        public RecipientDetails(@Nullable String name, @Nullable Long groupAvatarId,
                                boolean systemContact, @Nullable RecipientDatabase.RecipientSettings settings,
                                @Nullable List<SignalRecipient> participants) {
            this.groupAvatarId = groupAvatarId;
            this.systemContactPhoto = settings != null ? Util.uri(settings.getSystemContactPhotoUri()) : null;
            this.customLabel = settings != null ? settings.getSystemPhoneLabel() : null;
            this.contactUri = settings != null ? Util.uri(settings.getSystemContactUri()) : null;
            this.color = settings != null ? settings.getColor() : null;
            this.messageRingtone = settings != null ? settings.getMessageRingtone() : null;
            this.callRingtone = settings != null ? settings.getCallRingtone() : null;
            this.mutedUntil = settings != null ? settings.getMuteUntil() : 0;
            this.messageVibrateState = settings != null ? settings.getMessageVibrateState() : null;
            this.callVibrateState = settings != null ? settings.getCallVibrateState() : null;
            this.blocked = settings != null && settings.isBlocked();
            this.expireMessages = settings != null ? settings.getExpireMessages() : 0;
            this.participants = participants == null ? new LinkedList<>() : participants;
            this.profileName = settings != null ? settings.getProfileName() : null;
            this.seenInviteReminder = settings != null && settings.hasSeenInviteReminder();
            this.defaultSubscriptionId = settings != null ? settings.getDefaultSubscriptionId() : Optional.absent();
            this.registered = settings != null ? settings.getRegistered() : RecipientDatabase.RegisteredState.UNKNOWN;
            this.profileKey = settings != null ? settings.getProfileKey() : null;
            this.profileAvatar = settings != null ? settings.getProfileAvatar() : null;
            this.profileSharing = settings != null && settings.isProfileSharing();
            this.systemContact = systemContact;
            this.notificationChannel = settings != null ? settings.getNotificationChannel() : null;
            this.unidentifiedAccessMode = settings != null ? settings.getUnidentifiedAccessMode() : RecipientDatabase.UnidentifiedAccessMode.DISABLED;

            if (name == null && settings != null) this.name = settings.getSystemDisplayName();
            else this.name = name;
        }
    }

    private static class RecipientCache {

        private final Map<SignalAddress, SignalRecipient> cache = new SoftHashMap<>(1000);

        public synchronized SignalRecipient get(SignalAddress address) {
            return cache.get(address);
        }

        public synchronized void set(SignalAddress address, SignalRecipient recipient) {
            cache.put(address, recipient);
        }

    }
}
