package com.muzima.notifications;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;

import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.mms.OutgoingMediaMessage;
import com.muzima.messaging.sms.MessageSender;
import com.muzima.messaging.sms.OutgoingEncryptedMessage;
import com.muzima.messaging.sms.OutgoingTextMessage;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.MessagingDatabase.MarkedMessageInfo;
import com.muzima.messaging.sqlite.database.RecipientDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.model.SignalRecipient;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class RemoteReplyReceiver extends BroadcastReceiver {

    public static final String TAG = RemoteReplyReceiver.class.getSimpleName();
    public static final String REPLY_ACTION = "com.muzima.notifications.WEAR_REPLY";
    public static final String ADDRESS_EXTRA = "address";

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!REPLY_ACTION.equals(intent.getAction())) return;

        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);

        if (remoteInput == null) return;

        final SignalAddress address = intent.getParcelableExtra(ADDRESS_EXTRA);
        final CharSequence responseText = remoteInput.getCharSequence(MessageNotifier.EXTRA_REMOTE_REPLY);

        if (responseText != null) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    long threadId;

                    SignalRecipient recipient = SignalRecipient.from(context, address, false);
                    int subscriptionId = recipient.getDefaultSubscriptionId().or(-1);
                    long expiresIn = recipient.getExpireMessages() * 1000L;

                    if (recipient.isGroupRecipient()) {
                        OutgoingMediaMessage reply = new OutgoingMediaMessage(recipient, responseText.toString(), new LinkedList<>(), System.currentTimeMillis(), subscriptionId, expiresIn, 0, null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                        threadId = MessageSender.send(context, reply, -1, false, null);
                    } else if (TextSecurePreferences.isPushRegistered(context) && recipient.getRegistered() == RecipientDatabase.RegisteredState.REGISTERED) {
                        OutgoingEncryptedMessage reply = new OutgoingEncryptedMessage(recipient, responseText.toString(), expiresIn);
                        threadId = MessageSender.send(context, reply, -1, false, null);
                    } else {
                        OutgoingTextMessage reply = new OutgoingTextMessage(recipient, responseText.toString(), expiresIn, subscriptionId);
                        threadId = MessageSender.send(context, reply, -1, false, null);
                    }

                    List<MarkedMessageInfo> messageIds = DatabaseFactory.getThreadDatabase(context).setRead(threadId, true);

                    MessageNotifier.updateNotification(context);
                    MarkReadReceiver.process(context, messageIds);

                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

    }
}
