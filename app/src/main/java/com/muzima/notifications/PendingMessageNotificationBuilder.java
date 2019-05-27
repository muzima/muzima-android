package com.muzima.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.muzima.R;
import com.muzima.messaging.ConversationListActivity;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.preference.widgets.NotificationPrivacyPreference;
import com.muzima.messaging.sqlite.database.RecipientDatabase;

public class PendingMessageNotificationBuilder extends AbstractNotificationBuilder {

    public PendingMessageNotificationBuilder(Context context, NotificationPrivacyPreference privacy) {
        super(context, privacy);

        Intent intent = new Intent(context, ConversationListActivity.class);

        setSmallIcon(R.drawable.ic_launcher_logo_light);
        setColor(context.getResources().getColor(R.color.primary_blue));
        setCategory(NotificationCompat.CATEGORY_MESSAGE);

        setContentTitle(context.getString(R.string.message_notifier_pending_messages));
        setContentText(context.getString(R.string.message_notifier_you_have_pending_messages));
        setTicker(context.getString(R.string.message_notifier_you_have_pending_messages));

        setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));
        setAutoCancel(true);
        setAlarms(null, RecipientDatabase.VibrateState.DEFAULT);

        if (!NotificationChannels.supported()) {
            setPriority(TextSecurePreferences.getNotificationPriority(context));
        }
    }
}
