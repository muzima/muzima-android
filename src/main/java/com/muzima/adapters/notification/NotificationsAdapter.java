package com.muzima.adapters.notification;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
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
            convertView = layoutInflater.inflate(
                    R.layout.item_notifications_list, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.setStatus(getItem(position).getStatus());
        holder.setSubject(getItem(position).getSubject());
        holder.setReceiver(getItem(position).getPayload());
        return convertView;
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public class ViewHolder {
        private CheckedTextView subject;
        private TextView receiver;
        private String status;

        public ViewHolder(View convertView) {
            this.subject = (CheckedTextView) convertView.findViewById(R.id.subject);
            this.receiver = (TextView) convertView.findViewById(R.id.receiver);
        }

        public void setSubject(String text) {
            subject.setText(text);
            if (StringUtil.equals("read", status))
                subject.setTypeface(Fonts.roboto_medium(getContext()));
            else
                subject.setTypeface(Fonts.roboto_light(getContext()));

        }

        public void setReceiver(String text) {
            receiver.setText(text);
            if (StringUtil.equals("read", status))
                receiver.setTypeface(Fonts.roboto_medium(getContext()));
            else
                receiver.setTypeface(Fonts.roboto_light(getContext()));
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
