package com.muzima.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;

import com.muzima.R;
import com.muzima.messaging.preference.widgets.NotificationPrivacyPreference;
import com.muzima.messaging.sqlite.database.RecipientDatabase;

public class FailedNotificationBuilder extends AbstractNotificationBuilder {

    public FailedNotificationBuilder(Context context, NotificationPrivacyPreference privacy, Intent intent) {
        super(context, privacy);

        setSmallIcon(R.drawable.ic_launcher_logo_light);
        setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_action_warning_red));
        setContentTitle(context.getString(R.string.message_notifier_message_delivery_failed));
        setContentText(context.getString(R.string.message_notifier_failed_to_deliver_message));
        setTicker(context.getString(R.string.message_notifier_error_delivering_message));
        setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));
        setAutoCancel(true);
        setAlarms(null, RecipientDatabase.VibrateState.DEFAULT);
        setChannelId(NotificationChannels.FAILURES);
    }

}
