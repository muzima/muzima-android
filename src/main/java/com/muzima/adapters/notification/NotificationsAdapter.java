package com.muzima.adapters.notification;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Notification;
import com.muzima.controller.NotificationController;
import com.muzima.search.api.util.StringUtil;
import com.muzima.utils.Fonts;

/**
 * Responsible for displaying Notifications as list.
 */
public abstract class NotificationsAdapter extends ListAdapter<Notification> {
    protected NotificationController notificationController;
    protected BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    public NotificationsAdapter(Context context, int textViewResourceId, NotificationController notificationController) {
        super(context, textViewResourceId);
        this.notificationController = notificationController;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_notifications_list, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.setStatus(getItem(position).getStatus());
        holder.setSubject(getItem(position).getSubject());
        holder.setReceiver(getItem(position).getPayload());
        holder.markUnreadNotification();
        return convertView;
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public class ViewHolder {
        private CheckedTextView subject;
        private TextView receiver;
        private String status;
        private ImageView newNotificationImg;

        public ViewHolder(View convertView) {
            this.subject = (CheckedTextView) convertView.findViewById(R.id.subject);
            this.receiver = (TextView) convertView.findViewById(R.id.receiver);
            this.newNotificationImg = (ImageView) convertView.findViewById(R.id.newNotificationImg);
        }

        public void setSubject(String text) {
            subject.setText(text);
        }

        public void setReceiver(String text) {
            receiver.setText(text);
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public void markUnreadNotification() {
            if (StringUtil.equals("read", status)){
                subject.setTypeface(Fonts.roboto_light(getContext()));
                receiver.setTypeface(Fonts.roboto_light(getContext()));
                newNotificationImg.setVisibility(View.GONE);
            } else {
                subject.setTypeface(Fonts.roboto_medium(getContext()));
                receiver.setTypeface(Fonts.roboto_medium(getContext()));
                newNotificationImg.setVisibility(View.VISIBLE);
            }

        }
    }
}
