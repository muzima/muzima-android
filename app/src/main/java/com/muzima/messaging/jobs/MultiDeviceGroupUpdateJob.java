package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.crypto.UnidentifiedAccessUtil;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.jobmanager.dependencies.InjectableType;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.GroupDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.utils.GroupUtil;
import com.muzima.model.SignalRecipient;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentStream;
import org.whispersystems.signalservice.api.messages.multidevice.DeviceGroup;
import org.whispersystems.signalservice.api.messages.multidevice.DeviceGroupsOutputStream;
import org.whispersystems.signalservice.api.messages.multidevice.SignalServiceSyncMessage;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class MultiDeviceGroupUpdateJob extends ContextJob implements InjectableType {

    private static final long serialVersionUID = 1L;
    private static final String TAG = MultiDeviceGroupUpdateJob.class.getSimpleName();

    @Inject
    transient SignalServiceMessageSender messageSender;

    public MultiDeviceGroupUpdateJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public MultiDeviceGroupUpdateJob(Context context) {
        super(context, JobParameters.newBuilder()
                .withNetworkRequirement()
                .withGroupId(MultiDeviceGroupUpdateJob.class.getSimpleName())
                .create());
    }

    @Override
    protected void initialize(@NonNull SafeData data) {
    }

    @Override
    protected @NonNull
    Data serialize(@NonNull Data.Builder dataBuilder) {
        return dataBuilder.build();
    }

    @Override
    public void onRun() throws Exception {
        if (!TextSecurePreferences.isMultiDevice(context)) {
            Log.i(TAG, "Not multi device, aborting...");
            return;
        }

        File contactDataFile = createTempFile("multidevice-contact-update");
        GroupDatabase.Reader reader          = null;

        GroupDatabase.GroupRecord record;

        try {
            DeviceGroupsOutputStream out = new DeviceGroupsOutputStream(new FileOutputStream(contactDataFile));

            reader = DatabaseFactory.getGroupDatabase(context).getGroups();

            while ((record = reader.getNext()) != null) {
                if (!record.isMms()) {
                    List<String> members = new LinkedList<>();

                    for (SignalAddress member : record.getMembers()) {
                        members.add(member.serialize());
                    }

                    SignalRecipient recipient       = SignalRecipient.from(context, SignalAddress.fromSerialized(GroupUtil.getEncodedId(record.getId(), record.isMms())), false);
                    Optional<Integer> expirationTimer = recipient.getExpireMessages() > 0 ? Optional.of(recipient.getExpireMessages()) : Optional.absent();

                    out.write(new DeviceGroup(record.getId(), Optional.fromNullable(record.getTitle()),
                            members, getAvatar(record.getAvatar()),
                            record.isActive(), expirationTimer,
                            Optional.of(recipient.getColor().serialize()),
                            recipient.isBlocked()));
                }
            }

            out.close();

            if (contactDataFile.exists() && contactDataFile.length() > 0) {
                sendUpdate(messageSender, contactDataFile);
            } else {
                Log.w(TAG, "No groups present for sync message...");
            }

        } finally {
            if (contactDataFile != null) contactDataFile.delete();
            if (reader != null)          reader.close();
        }

    }

    @Override
    public boolean onShouldRetry(Exception exception) {
        if (exception instanceof PushNetworkException) return true;
        return false;
    }

    @Override
    public void onCanceled() {

    }

    private void sendUpdate(SignalServiceMessageSender messageSender, File contactsFile)
            throws IOException, UntrustedIdentityException
    {
        FileInputStream contactsFileStream = new FileInputStream(contactsFile);
        SignalServiceAttachmentStream attachmentStream   = SignalServiceAttachment.newStreamBuilder()
                .withStream(contactsFileStream)
                .withContentType("application/octet-stream")
                .withLength(contactsFile.length())
                .build();

        messageSender.sendMessage(SignalServiceSyncMessage.forGroups(attachmentStream),
                UnidentifiedAccessUtil.getAccessForSync(context));
    }


    private Optional<SignalServiceAttachmentStream> getAvatar(@Nullable byte[] avatar) {
        if (avatar == null) return Optional.absent();

        return Optional.of(SignalServiceAttachment.newStreamBuilder()
                .withStream(new ByteArrayInputStream(avatar))
                .withContentType("image/*")
                .withLength(avatar.length)
                .build());
    }

    private File createTempFile(String prefix) throws IOException {
        File file = File.createTempFile(prefix, "tmp", context.getCacheDir());
        file.deleteOnExit();

        return file;
    }
}
