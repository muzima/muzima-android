/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.notification;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Notification;
import com.muzima.controller.NotificationController;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.Fonts;
import com.muzima.utils.StringUtils;

import java.util.Date;

/**
 * Responsible for displaying Notifications as list.
 */
public abstract class NotificationAdapter extends ListAdapter<Notification> {
    final NotificationController notificationController;
    BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    NotificationAdapter(Context context, int textViewResourceId, NotificationController notificationController) {
        super(context, textViewResourceId);
        this.notificationController = notificationController;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
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
        holder.setNotificationDate(getItem(position).getDateCreated());
        holder.markUnreadNotification();
        return convertView;
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    class ViewHolder {
        private final TextView subject;
        private final TextView notificationDate;
        private String status;
        private final ImageView newNotificationImg;

        ViewHolder(View convertView) {
            this.subject = convertView.findViewById(R.id.subject);
            this.notificationDate = convertView.findViewById(R.id.notificationDate);
            this.newNotificationImg = convertView.findViewById(R.id.newNotificationImg);
        }

        void setSubject(String text) {
            subject.setText(text);
        }

        void setNotificationDate(Date date) {
            notificationDate.setText(DateUtils.getMonthNameFormattedDate(date));
        }

        void setStatus(String status) {
            this.status = status;
        }

        void markUnreadNotification() {
            if (StringUtils.equals(Constants.NotificationStatusConstants.NOTIFICATION_READ, status)){
                subject.setTypeface(Fonts.roboto_light(getContext()));
                notificationDate.setTypeface(Fonts.roboto_light(getContext()));
                newNotificationImg.setVisibility(View.GONE);
            } else {
                subject.setTypeface(Fonts.roboto_medium(getContext()));
                notificationDate.setTypeface(Fonts.roboto_medium(getContext()));
                newNotificationImg.setVisibility(View.VISIBLE);
            }
        }
    }
}
