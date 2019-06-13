package com.muzima.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.muzima.R;
import com.muzima.messaging.ConversationListActivity;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.preference.widgets.NotificationPrivacyPreference;
import com.muzima.messaging.utils.Util;
import com.muzima.model.SignalRecipient;

import java.util.LinkedList;
import java.util.List;

public class MultipleRecipientNotificationBuilder extends AbstractNotificationBuilder {

    private final List<CharSequence> messageBodies = new LinkedList<>();

    public MultipleRecipientNotificationBuilder(Context context, NotificationPrivacyPreference privacy) {
        super(context, privacy);

        setColor(context.getResources().getColor(R.color.primary_color));
        setSmallIcon(R.drawable.ic_launcher_logo_light);
        setContentTitle(context.getString(R.string.app_name));
        setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, ConversationListActivity.class), 0));
        setCategory(NotificationCompat.CATEGORY_MESSAGE);
        setGroupSummary(true);

        if (!NotificationChannels.supported()) {
            setPriority(TextSecurePreferences.getNotificationPriority(context));
        }
    }

    public void setMessageCount(int messageCount, int threadCount) {
        setSubText(context.getString(R.string.hint_d_new_messages_in_d_conversations,
                messageCount, threadCount));
        setContentInfo(String.valueOf(messageCount));
        setNumber(messageCount);
    }

    public void setMostRecentSender(SignalRecipient recipient) {
        if (privacy.isDisplayContact()) {
            setContentText(context.getString(R.string.general_most_recent_from_s,
                    recipient.toShortString()));
        }

        if (recipient.getNotificationChannel() != null) {
            setChannelId(recipient.getNotificationChannel());
        }
    }

    public void addActions(PendingIntent markAsReadIntent) {
        NotificationCompat.Action markAllAsReadAction = new NotificationCompat.Action(R.drawable.check,
                context.getString(R.string.general_mark_all_as_read),
                markAsReadIntent);
        addAction(markAllAsReadAction);
        extend(new NotificationCompat.WearableExtender().addAction(markAllAsReadAction));
    }

    public void addMessageBody(@NonNull SignalRecipient sender, @Nullable CharSequence body) {
        if (privacy.isDisplayMessage()) {
            messageBodies.add(getStyledMessage(sender, body));
        } else if (privacy.isDisplayContact()) {
            messageBodies.add(Util.getBoldedString(sender.toShortString()));
        }

        if (privacy.isDisplayContact() && sender.getContactUri() != null) {
            addPerson(sender.getContactUri().toString());
        }
    }

    @Override
    public Notification build() {
        if (privacy.isDisplayMessage() || privacy.isDisplayContact()) {
            NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();

            for (CharSequence body : messageBodies) {
                style.addLine(body);
            }

            setStyle(style);
        }

        return super.build();
    }
}
