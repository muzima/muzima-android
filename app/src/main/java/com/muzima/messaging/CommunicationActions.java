package com.muzima.messaging;

import android.Manifest;
import android.app.Activity;
import android.app.TaskStackBuilder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.webrtc.view.WebRtcCallActivity;
import com.muzima.model.SignalRecipient;
import com.muzima.service.WebRtcCallService;
import com.muzima.utils.Permissions;

public class CommunicationActions {
    public static void startVoiceCall(@NonNull Activity activity, @NonNull SignalRecipient recipient) {
        Permissions.with(activity)
                .request(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                .ifNecessary()
                .withRationaleDialog(activity.getString(R.string.general_to_call_s_signal_needs_access_to_your_microphone_and_camera, recipient.toShortString()),
                        R.drawable.ic_mic_white_48dp,
                        R.drawable.ic_videocam_white_48dp)
                .withPermanentDenialDialog(activity.getString(R.string.general_signal_needs_the_microphone_and_camera_permissions_in_order_to_call_s, recipient.toShortString()))
                .onAllGranted(() -> {
                    Intent intent = new Intent(activity, WebRtcCallService.class);
                    intent.setAction(WebRtcCallService.ACTION_OUTGOING_CALL);
                    intent.putExtra(WebRtcCallService.EXTRA_REMOTE_ADDRESS, recipient.getAddress());
                    activity.startService(intent);

                    Intent activityIntent = new Intent(activity, WebRtcCallActivity.class);
                    activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(activityIntent);
                })
                .execute();
    }

    public static void startConversation(@NonNull Context context, @NonNull SignalRecipient recipient, @Nullable String text) {
        startConversation(context, recipient, text, null);
    }

    public static void startConversation(@NonNull Context context, @NonNull  SignalRecipient recipient, @Nullable String text, @Nullable TaskStackBuilder backStack)
    {
        new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(Void... voids) {
                return DatabaseFactory.getThreadDatabase(context).getThreadIdFor(recipient);
            }

            @Override
            protected void onPostExecute(Long threadId) {
                Intent intent = new Intent(context, ConversationActivity.class);
                intent.putExtra(ConversationActivity.ADDRESS_EXTRA, recipient.getAddress());
                intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, threadId);
                intent.putExtra(ConversationActivity.TIMING_EXTRA, System.currentTimeMillis());

                if (!TextUtils.isEmpty(text)) {
                    intent.putExtra(ConversationActivity.TEXT_EXTRA, text);
                }

                if (backStack != null) {
                    backStack.addNextIntent(intent);
                    backStack.startActivities();
                } else {
                    context.startActivity(intent);
                }
            }
        }.execute();
    }

    public static void composeSmsThroughDefaultApp(@NonNull Context context, @NonNull SignalAddress address, @Nullable String text) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + address.serialize()));
        if (text != null) {
            intent.putExtra("sms_body", text);
        }
        context.startActivity(intent);
    }

    public static void openBrowserLink(@NonNull Context context, @NonNull String link) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.general_no_browser_found, Toast.LENGTH_SHORT).show();
        }
    }
}
