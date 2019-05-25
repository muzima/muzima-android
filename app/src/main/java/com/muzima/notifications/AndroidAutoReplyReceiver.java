package com.muzima.notifications;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;

import com.muzima.messaging.mms.OutgoingMediaMessage;
import com.muzima.messaging.sms.MessageSender;
import com.muzima.messaging.sms.OutgoingTextMessage;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.MessagingDatabase.MarkedMessageInfo;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.model.SignalRecipient;

import org.whispersystems.libsignal.logging.Log;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class AndroidAutoReplyReceiver
        extends BroadcastReceiver {

    public static final String TAG             = AndroidAutoReplyReceiver.class.getSimpleName();
    public static final String REPLY_ACTION    = "com.muzima.notifications.ANDROID_AUTO_REPLY";
    public static final String ADDRESS_EXTRA   = "car_address";
    public static final String VOICE_REPLY_KEY = "car_voice_reply_key";
    public static final String THREAD_ID_EXTRA = "car_reply_thread_id";

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onReceive(final Context context, Intent intent)
    {
        if (!REPLY_ACTION.equals(intent.getAction())) return;

        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);

        if (remoteInput == null) return;

        final SignalAddress address      = intent.getParcelableExtra(ADDRESS_EXTRA);
        final long         threadId     = intent.getLongExtra(THREAD_ID_EXTRA, -1);
        final CharSequence responseText = getMessageText(intent);
        final SignalRecipient recipient    = SignalRecipient.from(context, address, false);

        if (responseText != null) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {

                    long replyThreadId;

                    int  subscriptionId = recipient.getDefaultSubscriptionId().or(-1);
                    long expiresIn      = recipient.getExpireMessages() * 1000L;

                    if (recipient.isGroupRecipient()) {
                        Log.w("AndroidAutoReplyReceiver", "GroupRecipient, Sending media message");
                        OutgoingMediaMessage reply = new OutgoingMediaMessage(recipient, responseText.toString(), new LinkedList<>(), System.currentTimeMillis(), subscriptionId, expiresIn, 0, null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                        replyThreadId = MessageSender.send(context, reply, threadId, false, null);
                    } else {
                        Log.w("AndroidAutoReplyReceiver", "Sending regular message ");
                        OutgoingTextMessage reply = new OutgoingTextMessage(recipient, responseText.toString(), expiresIn, subscriptionId);
                        replyThreadId = MessageSender.send(context, reply, threadId, false, null);
                    }

                    List<MarkedMessageInfo> messageIds = DatabaseFactory.getThreadDatabase(context).setRead(replyThreadId, true);

                    MessageNotifier.updateNotification(context);
                    MarkReadReceiver.process(context, messageIds);

                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private CharSequence getMessageText(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence(VOICE_REPLY_KEY);
        }
        return null;
    }
}
