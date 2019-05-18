package com.muzima.model;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.function.Consumer;
import com.muzima.R;
import com.muzima.messaging.RecipientModifiedListener;
import com.muzima.messaging.RecipientProvider;
import com.muzima.messaging.ResourceContactPhoto;
import com.muzima.messaging.contacts.avatars.ContactColors;
import com.muzima.messaging.contacts.avatars.GroupRecordContactPhoto;
import com.muzima.messaging.contacts.avatars.ProfileContactPhoto;
import com.muzima.messaging.contacts.avatars.SystemContactPhoto;
import com.muzima.messaging.contacts.avatars.TransparentContactPhoto;
import com.muzima.messaging.contacts.FallbackContactPhoto;
import com.muzima.messaging.contacts.avatars.ContactPhoto;
import com.muzima.messaging.contacts.avatars.GeneratedContactPhoto;
import com.muzima.messaging.sqlite.database.GroupDatabase;
import com.muzima.messaging.sqlite.database.RecipientDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.utils.FutureTaskListener;
import com.muzima.messaging.utils.Util;
import com.muzima.notifications.NotificationChannels;
import com.muzima.utils.ListenableFutureTask;
import com.muzima.utils.MaterialColor;

import org.whispersystems.libsignal.util.guava.Optional;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;

public class SignalRecipient implements RecipientModifiedListener {
    private static final String TAG = SignalRecipient.class.getSimpleName();
    private static final RecipientProvider provider = new RecipientProvider();

    private final Set<RecipientModifiedListener> listeners = Collections.newSetFromMap(new WeakHashMap<RecipientModifiedListener, Boolean>());

    private final @NonNull
    SignalAddress address;
    private final @NonNull
    List<SignalRecipient> participants = new LinkedList<>();

    private @Nullable
    String name;
    private @Nullable
    String customLabel;
    private boolean resolving;

    private @Nullable
    Uri systemContactPhoto;
    private @Nullable
    Long groupAvatarId;
    private Uri contactUri;
    private @Nullable
    Uri messageRingtone = null;
    private @Nullable
    Uri callRingtone = null;
    private long mutedUntil = 0;
    private boolean blocked = false;
    private RecipientDatabase.VibrateState messageVibrate = RecipientDatabase.VibrateState.DEFAULT;
    private RecipientDatabase.VibrateState callVibrate = RecipientDatabase.VibrateState.DEFAULT;
    private int expireMessages = 0;
    private Optional<Integer> defaultSubscriptionId = Optional.absent();
    private @NonNull
    RecipientDatabase.RegisteredState registered = RecipientDatabase.RegisteredState.UNKNOWN;

    private @Nullable
    MaterialColor color;
    private boolean seenInviteReminder;
    private @Nullable
    byte[] profileKey;
    private @Nullable
    String profileName;
    private @Nullable
    String profileAvatar;
    private boolean profileSharing;
    private String notificationChannel;

    private @NonNull
    RecipientDatabase.UnidentifiedAccessMode unidentifiedAccessMode = RecipientDatabase.UnidentifiedAccessMode.DISABLED;

    @SuppressWarnings("ConstantConditions")
    public static @NonNull
    SignalRecipient from(@NonNull Context context, @NonNull SignalAddress address, boolean asynchronous) {
        if (address == null) throw new AssertionError(address);
        return provider.getRecipient(context, address, Optional.absent(), Optional.absent(), asynchronous);
    }

    @SuppressWarnings("ConstantConditions")
    public static @NonNull
    SignalRecipient from(@NonNull Context context, @NonNull SignalAddress address, @NonNull Optional<RecipientDatabase.RecipientSettings> settings, @NonNull Optional<GroupDatabase.GroupRecord> groupRecord, boolean asynchronous) {
        if (address == null) throw new AssertionError(address);
        return provider.getRecipient(context, address, settings, groupRecord, asynchronous);
    }

    public static void applyCached(@NonNull SignalAddress address, Consumer<SignalRecipient> consumer) {
        Optional<SignalRecipient> recipient = provider.getCached(address);
        if (recipient.isPresent()) consumer.accept(recipient.get());
    }

    public SignalRecipient(@NonNull SignalAddress address,
                           @Nullable SignalRecipient stale,
                           @NonNull Optional<RecipientProvider.RecipientDetails> details,
                           @NonNull ListenableFutureTask<RecipientProvider.RecipientDetails> future) {
        this.address = address;
        this.color = null;
        this.resolving = true;

        if (stale != null) {
            this.name = stale.name;
            this.contactUri = stale.contactUri;
            this.systemContactPhoto = stale.systemContactPhoto;
            this.groupAvatarId = stale.groupAvatarId;
            this.color = MaterialColor.BLUE_A200;
            this.customLabel = stale.customLabel;
            this.messageRingtone = stale.messageRingtone;
            this.callRingtone = stale.callRingtone;
            this.mutedUntil = stale.mutedUntil;
            this.blocked = stale.blocked;
            this.messageVibrate = stale.messageVibrate;
            this.callVibrate = stale.callVibrate;
            this.expireMessages = stale.expireMessages;
            this.seenInviteReminder = stale.seenInviteReminder;
            this.defaultSubscriptionId = stale.defaultSubscriptionId;
            this.registered = stale.registered;
            this.notificationChannel = stale.notificationChannel;
            this.profileKey = stale.profileKey;
            this.profileName = stale.profileName;
            this.profileAvatar = stale.profileAvatar;
            this.profileSharing = stale.profileSharing;
            this.unidentifiedAccessMode = stale.unidentifiedAccessMode;

            this.participants.clear();
            this.participants.addAll(stale.participants);
        }

        if (details.isPresent()) {
            this.name = details.get().name;
            this.systemContactPhoto = details.get().systemContactPhoto;
            this.groupAvatarId = details.get().groupAvatarId;
            this.color = MaterialColor.BLUE_A200;
            this.messageRingtone = details.get().messageRingtone;
            this.callRingtone = details.get().callRingtone;
            this.mutedUntil = details.get().mutedUntil;
            this.blocked = details.get().blocked;
            this.messageVibrate = details.get().messageVibrateState;
            this.callVibrate = details.get().callVibrateState;
            this.expireMessages = details.get().expireMessages;
            this.seenInviteReminder = details.get().seenInviteReminder;
            this.defaultSubscriptionId = details.get().defaultSubscriptionId;
            this.registered = details.get().registered;
            this.notificationChannel = details.get().notificationChannel;
            this.profileKey = details.get().profileKey;
            this.profileName = details.get().profileName;
            this.profileAvatar = details.get().profileAvatar;
            this.profileSharing = details.get().profileSharing;
            this.unidentifiedAccessMode = details.get().unidentifiedAccessMode;

            this.participants.clear();
            this.participants.addAll(details.get().participants);
        }

        future.addListener(new FutureTaskListener<RecipientProvider.RecipientDetails>() {
            @Override
            public void onSuccess(RecipientProvider.RecipientDetails result) {
                if (result != null) {
                    synchronized (SignalRecipient.this) {
                        SignalRecipient.this.name = result.name;
                        SignalRecipient.this.contactUri = result.contactUri;
                        SignalRecipient.this.systemContactPhoto = result.systemContactPhoto;
                        SignalRecipient.this.groupAvatarId = result.groupAvatarId;
                        SignalRecipient.this.color = MaterialColor.BLUE_A200;
                        SignalRecipient.this.customLabel = result.customLabel;
                        SignalRecipient.this.messageRingtone = result.messageRingtone;
                        SignalRecipient.this.callRingtone = result.callRingtone;
                        SignalRecipient.this.mutedUntil = result.mutedUntil;
                        SignalRecipient.this.blocked = result.blocked;
                        SignalRecipient.this.messageVibrate = result.messageVibrateState;
                        SignalRecipient.this.callVibrate = result.callVibrateState;
                        SignalRecipient.this.expireMessages = result.expireMessages;
                        SignalRecipient.this.seenInviteReminder = result.seenInviteReminder;
                        SignalRecipient.this.defaultSubscriptionId = result.defaultSubscriptionId;
                        SignalRecipient.this.registered = result.registered;
                        SignalRecipient.this.notificationChannel = result.notificationChannel;
                        SignalRecipient.this.profileKey = result.profileKey;
                        SignalRecipient.this.profileName = result.profileName;
                        SignalRecipient.this.profileAvatar = result.profileAvatar;
                        SignalRecipient.this.profileSharing = result.profileSharing;
                        SignalRecipient.this.profileName = result.profileName;
                        SignalRecipient.this.unidentifiedAccessMode = result.unidentifiedAccessMode;

                        SignalRecipient.this.participants.clear();
                        SignalRecipient.this.participants.addAll(result.participants);
                        SignalRecipient.this.resolving = false;

                        if (!listeners.isEmpty()) {
                            for (SignalRecipient recipient : participants)
                                recipient.addListener(SignalRecipient.this);
                        }

                        SignalRecipient.this.notifyAll();
                    }

                    notifyListeners();
                }
            }

            @Override
            public void onFailure(ExecutionException error) {
                Log.w(TAG, error);
            }
        });
    }

    public SignalRecipient(@NonNull SignalAddress address, @NonNull RecipientProvider.RecipientDetails details) {
        this.address = address;
        this.contactUri = details.contactUri;
        this.name = details.name;
        this.systemContactPhoto = details.systemContactPhoto;
        this.groupAvatarId = details.groupAvatarId;
        this.color = MaterialColor.BLUE_A200;
        this.customLabel = details.customLabel;
        this.messageRingtone = details.messageRingtone;
        this.callRingtone = details.callRingtone;
        this.mutedUntil = details.mutedUntil;
        this.blocked = details.blocked;
        this.messageVibrate = details.messageVibrateState;
        this.callVibrate = details.callVibrateState;
        this.expireMessages = details.expireMessages;
        this.seenInviteReminder = details.seenInviteReminder;
        this.defaultSubscriptionId = details.defaultSubscriptionId;
        this.registered = details.registered;
        this.notificationChannel = details.notificationChannel;
        this.profileKey = details.profileKey;
        this.profileName = details.profileName;
        this.profileAvatar = details.profileAvatar;
        this.profileSharing = details.profileSharing;
        this.unidentifiedAccessMode = details.unidentifiedAccessMode;

        this.participants.addAll(details.participants);
        this.resolving = false;
    }

    public synchronized @Nullable
    Uri getContactUri() {
        return this.contactUri;
    }

    public void setContactUri(@Nullable Uri contactUri) {
        boolean notify = false;

        synchronized (this) {
            if (!Util.equals(contactUri, this.contactUri)) {
                this.contactUri = contactUri;
                notify = true;
            }
        }

        if (notify) notifyListeners();
    }

    public synchronized @Nullable
    String getName() {
        if (this.name == null && isMmsGroupRecipient()) {
            List<String> names = new LinkedList<>();

            for (SignalRecipient recipient : participants) {
                names.add(recipient.toShortString());
            }

            return Util.join(names, ", ");
        }

        return this.name;
    }

    public void setName(@Nullable String name) {
        boolean notify = false;

        synchronized (this) {
            if (!Util.equals(this.name, name)) {
                this.name = name;
                notify = true;
            }
        }

        if (notify) notifyListeners();
    }

    public synchronized @NonNull
    MaterialColor getColor() {
        if (isGroupRecipient()) return MaterialColor.GROUP;
        else if (color != null) return color;
        else if (name != null) return ContactColors.generateFor(name);
        else return ContactColors.UNKNOWN_COLOR;
    }

    public void setColor(@NonNull MaterialColor color) {
        synchronized (this) {
            this.color = color;
        }

        notifyListeners();
    }

    public @NonNull
    SignalAddress getAddress() {
        return address;
    }

    public synchronized @Nullable
    String getCustomLabel() {
        return customLabel;
    }

    public void setCustomLabel(@Nullable String customLabel) {
        boolean notify = false;

        synchronized (this) {
            if (!Util.equals(customLabel, this.customLabel)) {
                this.customLabel = customLabel;
                notify = true;
            }
        }

        if (notify) notifyListeners();
    }

    public synchronized Optional<Integer> getDefaultSubscriptionId() {
        return defaultSubscriptionId;
    }

    public void setDefaultSubscriptionId(Optional<Integer> defaultSubscriptionId) {
        synchronized (this) {
            this.defaultSubscriptionId = defaultSubscriptionId;
        }

        notifyListeners();
    }

    public synchronized @Nullable
    String getProfileName() {
        return profileName;
    }

    public void setProfileName(@Nullable String profileName) {
        synchronized (this) {
            this.profileName = profileName;
        }

        notifyListeners();
    }

    public synchronized @Nullable
    String getProfileAvatar() {
        return profileAvatar;
    }

    public void setProfileAvatar(@Nullable String profileAvatar) {
        synchronized (this) {
            this.profileAvatar = profileAvatar;
        }

        notifyListeners();
    }

    public synchronized boolean isProfileSharing() {
        return profileSharing;
    }

    public void setProfileSharing(boolean value) {
        synchronized (this) {
            this.profileSharing = value;
        }

        notifyListeners();
    }

    public boolean isGroupRecipient() {
        return address.isGroup();
    }

    public boolean isMmsGroupRecipient() {
        return address.isMmsGroup();
    }

    public boolean isPushGroupRecipient() {
        return address.isGroup() && !address.isMmsGroup();
    }

    public @NonNull
    synchronized List<SignalRecipient> getParticipants() {
        return new LinkedList<>(participants);
    }

    public void setParticipants(@NonNull List<SignalRecipient> participants) {
        synchronized (this) {
            this.participants.clear();
            this.participants.addAll(participants);
        }

        notifyListeners();
    }

    public synchronized void addListener(RecipientModifiedListener listener) {
        if (listeners.isEmpty()) {
            for (SignalRecipient recipient : participants) recipient.addListener(this);
        }
        listeners.add(listener);
    }

    public synchronized void removeListener(RecipientModifiedListener listener) {
        listeners.remove(listener);

        if (listeners.isEmpty()) {
            for (SignalRecipient recipient : participants) recipient.removeListener(this);
        }
    }

    public synchronized String toShortString() {
        return (getName() == null ? address.serialize() : getName());
    }

    public synchronized @NonNull
    Drawable getFallbackContactPhotoDrawable(Context context, boolean inverted) {
        return getFallbackContactPhoto().asDrawable(context, getColor().toAvatarColor(context), inverted);
    }

    public synchronized @NonNull
    FallbackContactPhoto getFallbackContactPhoto() {
        if (isResolving()) return new TransparentContactPhoto();
        else if (isGroupRecipient())
            return new ResourceContactPhoto(R.drawable.ic_group_white_24dp, R.drawable.ic_group_large);
        else if (!TextUtils.isEmpty(name))
            return new GeneratedContactPhoto(name, R.drawable.ic_profile_default);
        else
            return new ResourceContactPhoto(R.drawable.ic_profile_default, R.drawable.ic_person_large);
    }

    public synchronized @Nullable
    ContactPhoto getContactPhoto() {
        if (isGroupRecipient() && groupAvatarId != null)
            return new GroupRecordContactPhoto(address, groupAvatarId);
        else if (systemContactPhoto != null)
            return new SystemContactPhoto(address, systemContactPhoto, 0);
        else if (profileAvatar != null) return new ProfileContactPhoto(address, profileAvatar);
        else return null;
    }

    public void setSystemContactPhoto(@Nullable Uri systemContactPhoto) {
        boolean notify = false;

        synchronized (this) {
            if (!Util.equals(systemContactPhoto, this.systemContactPhoto)) {
                this.systemContactPhoto = systemContactPhoto;
                notify = true;
            }
        }

        if (notify) notifyListeners();
    }

    public void setGroupAvatarId(@Nullable Long groupAvatarId) {
        boolean notify = false;

        synchronized (this) {
            if (!Util.equals(this.groupAvatarId, groupAvatarId)) {
                this.groupAvatarId = groupAvatarId;
                notify = true;
            }
        }

        if (notify) notifyListeners();
    }

    public synchronized @Nullable
    Uri getMessageRingtone() {
        if (messageRingtone != null && messageRingtone.getScheme() != null && messageRingtone.getScheme().startsWith("file")) {
            return null;
        }

        return messageRingtone;
    }

    public void setMessageRingtone(@Nullable Uri ringtone) {
        synchronized (this) {
            this.messageRingtone = ringtone;
        }

        notifyListeners();
    }

    public synchronized @Nullable
    Uri getCallRingtone() {
        if (callRingtone != null && callRingtone.getScheme() != null && callRingtone.getScheme().startsWith("file")) {
            return null;
        }

        return callRingtone;
    }

    public void setCallRingtone(@Nullable Uri ringtone) {
        synchronized (this) {
            this.callRingtone = ringtone;
        }

        notifyListeners();
    }

    public synchronized boolean isMuted() {
        return System.currentTimeMillis() <= mutedUntil;
    }

    public void setMuted(long mutedUntil) {
        synchronized (this) {
            this.mutedUntil = mutedUntil;
        }

        notifyListeners();
    }

    public synchronized boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        synchronized (this) {
            this.blocked = blocked;
        }

        notifyListeners();
    }

    public synchronized RecipientDatabase.VibrateState getMessageVibrate() {
        return messageVibrate;
    }

    public void setMessageVibrate(RecipientDatabase.VibrateState vibrate) {
        synchronized (this) {
            this.messageVibrate = vibrate;
        }

        notifyListeners();
    }

    public synchronized RecipientDatabase.VibrateState getCallVibrate() {
        return callVibrate;
    }

    public void setCallVibrate(RecipientDatabase.VibrateState vibrate) {
        synchronized (this) {
            this.callVibrate = vibrate;
        }

        notifyListeners();
    }

    public synchronized int getExpireMessages() {
        return expireMessages;
    }

    public void setExpireMessages(int expireMessages) {
        synchronized (this) {
            this.expireMessages = expireMessages;
        }

        notifyListeners();
    }

    public synchronized boolean hasSeenInviteReminder() {
        return seenInviteReminder;
    }

    public void setHasSeenInviteReminder(boolean value) {
        synchronized (this) {
            this.seenInviteReminder = value;
        }

        notifyListeners();
    }

    public synchronized RecipientDatabase.RegisteredState getRegistered() {
        if (isPushGroupRecipient()) return RecipientDatabase.RegisteredState.REGISTERED;
        else if (isMmsGroupRecipient()) return RecipientDatabase.RegisteredState.NOT_REGISTERED;

        return registered;
    }

    public void setRegistered(@NonNull RecipientDatabase.RegisteredState value) {
        boolean notify = false;

        synchronized (this) {
            if (this.registered != value) {
                this.registered = value;
                notify = true;
            }
        }

        if (notify) notifyListeners();
    }

    public synchronized @Nullable String getNotificationChannel() {
        return !NotificationChannels.supported() ? null : notificationChannel;
    }

    public void setNotificationChannel(@Nullable String value) {
        boolean notify = false;

        synchronized (this) {
            if (!Util.equals(this.notificationChannel, value)) {
                this.notificationChannel = value;
                notify = true;
            }
        }

        if (notify) notifyListeners();
    }

    public synchronized @Nullable
    byte[] getProfileKey() {
        return profileKey;
    }

    public void setProfileKey(@Nullable byte[] profileKey) {
        synchronized (this) {
            this.profileKey = profileKey;
        }

        notifyListeners();
    }

    public synchronized RecipientDatabase.UnidentifiedAccessMode getUnidentifiedAccessMode() {
        return unidentifiedAccessMode;
    }

    public void setUnidentifiedAccessMode(@NonNull RecipientDatabase.UnidentifiedAccessMode unidentifiedAccessMode) {
        synchronized (this) {
            this.unidentifiedAccessMode = unidentifiedAccessMode;
        }

        notifyListeners();
    }

    public synchronized boolean isSystemContact() {
        return contactUri != null;
    }

    public synchronized SignalRecipient resolve() {
        while (resolving) Util.wait(this, 0);
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof SignalRecipient)) return false;

        SignalRecipient that = (SignalRecipient) o;

        return this.address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    private void notifyListeners() {
        Set<RecipientModifiedListener> localListeners;

        synchronized (this) {
            localListeners = new HashSet<>(listeners);
        }

        for (RecipientModifiedListener listener : localListeners)
            listener.onModified(this);
    }

    @Override
    public void onModified(SignalRecipient recipient) {
        notifyListeners();
    }

    public synchronized boolean isResolving() {
        return resolving;
    }
}
