package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.annimon.stream.Stream;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.crypto.UnidentifiedAccessUtil;
import com.muzima.messaging.jobmanager.JobParameters;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.jobmanager.dependencies.InjectableType;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.utils.GroupUtil;
import com.muzima.model.SignalRecipient;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UnidentifiedAccessPair;
import org.whispersystems.signalservice.api.messages.SignalServiceTypingMessage;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class TypingSendJob extends ContextJob implements InjectableType {

    private static final String TAG = TypingSendJob.class.getSimpleName();

    private static final String KEY_THREAD_ID = "thread_id";
    private static final String KEY_TYPING    = "typing";

    private long    threadId;
    private boolean typing;

    @Inject
    SignalServiceMessageSender messageSender;

    public TypingSendJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public TypingSendJob(Context context, long threadId, boolean typing) {
        super(context, JobParameters.newBuilder()
                .withGroupId("TYPING_" + threadId)
                .withRetryCount(1)
                .create());

        this.threadId = threadId;
        this.typing   = typing;
    }

    @Override
    protected void initialize(@NonNull SafeData data) {
        this.threadId = data.getLong(KEY_THREAD_ID);
        this.typing   = data.getBoolean(KEY_TYPING);
    }

    @NonNull
    @Override
    protected Data serialize(@NonNull Data.Builder dataBuilder) {
        return dataBuilder.putLong(KEY_THREAD_ID, threadId)
                .putBoolean(KEY_TYPING, typing)
                .build();
    }

    @Override
    public void onRun() throws Exception {
        if (!TextSecurePreferences.isTypingIndicatorsEnabled(context)) {
            return;
        }

        Log.d(TAG, "Sending typing " + (typing ? "started" : "stopped") + " for thread " + threadId);

        SignalRecipient recipient = DatabaseFactory.getThreadDatabase(context).getRecipientForThreadId(threadId);

        if (recipient == null) {
            throw new IllegalStateException("Tried to send a typing indicator to a non-existent thread.");
        }

        List<SignalRecipient> recipients = Collections.singletonList(recipient);
        Optional<byte[]> groupId    = Optional.absent();

        if (recipient.isGroupRecipient()) {
            recipients = DatabaseFactory.getGroupDatabase(context).getGroupMembers(recipient.getAddress().toGroupString(), false);
            groupId    = Optional.of(GroupUtil.getDecodedId(recipient.getAddress().toGroupString()));
        }

        List<SignalServiceAddress>             addresses          = Stream.of(recipients).map(r -> new SignalServiceAddress(r.getAddress().serialize())).toList();
        List<Optional<UnidentifiedAccessPair>> unidentifiedAccess = Stream.of(recipients).map(r -> UnidentifiedAccessUtil.getAccessFor(context, r)).toList();
        SignalServiceTypingMessage typingMessage      = new SignalServiceTypingMessage(typing ? SignalServiceTypingMessage.Action.STARTED : SignalServiceTypingMessage.Action.STOPPED, System.currentTimeMillis(), groupId);

        messageSender.sendTyping(addresses, unidentifiedAccess, typingMessage);
    }

    @Override
    protected void onCanceled() {
    }

    @Override
    protected boolean onShouldRetry(Exception exception) {
        return false;
    }
}
