package com.muzima.adapters.providers;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Provider;
import com.muzima.controller.NotificationController;
import com.muzima.controller.ProviderController;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.Fonts;
import com.muzima.utils.StringUtils;

import org.apache.lucene.queryParser.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.muzima.utils.Constants.NotificationStatusConstants.NOTIFICATION_UNREAD;

public abstract class ProvidersAdapter extends ListAdapter<Provider> {

    protected ProviderController providerController;
    protected NotificationController notificationController;
    protected MuzimaApplication muzimaApplication;
    protected BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    public ProvidersAdapter(Context context, int textViewResourceId, MuzimaApplication muzimaApplication) {
        super(context, textViewResourceId);
        this.providerController = muzimaApplication.getProviderController();
        this.notificationController = muzimaApplication.getNotificationController();
        this.muzimaApplication = muzimaApplication;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ProvidersAdapter.ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext( ));
            convertView = layoutInflater.inflate(R.layout.item_providers_list, parent, false);
            holder = new ProvidersAdapter.ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ProvidersAdapter.ViewHolder) convertView.getTag( );
        }
        holder.setIdentifier(getItem(position).getIdentifier( ));
        try {
            int notif = notificationController.getAllNotificationsBySenderCount(getItem(position).getPerson().getUuid(),NOTIFICATION_UNREAD);
            if(notif>0){
                holder.setId(String.valueOf(notif));
            }else{
                holder.setId("");
            }
        } catch (NotificationController.NotificationFetchException e) {
            e.printStackTrace( );
        } catch (ParseException e) {
            e.printStackTrace( );
        }
        holder.setName(getItem(position).getName( ));
        holder.markUnreadNotification( );

//        Notification replyNotification = new Notification();
//        replyNotification.setStatus(Constants.NotificationStatusConstants.NOTIFICATION_UNREAD);
//        replyNotification.setPatient(new Patient());
//        replyNotification.setReceiver(getItem(position).getPerson());
//        replyNotification.setSender(muzimaApplication.getAuthenticatedUser().getPerson());
//        replyNotification.setSource("mUzima Mobile Device");
//        replyNotification.setDateCreated(new Date());
//        replyNotification.setUuid(UUID.randomUUID().toString());
//        replyNotification.setSubject("No Subject");
//        replyNotification.setUploadStatus(Constants.NotificationStatusConstants.NOTIFICATION_NOT_UPLOADED);
//        replyNotification.setPayload("Message from super User");
//        try {
//            notificationController.saveNotification(replyNotification);
//        } catch (NotificationController.NotificationSaveException e) {
//            e.printStackTrace( );
//        }

        List<Notification> allMessagesThreadForPerson = new ArrayList<>();
        List<Notification> notificationBySender = new ArrayList<>();
        List<Notification> notificationByReceiver = new ArrayList<>();
        try {
            notificationBySender = notificationController.getAllNotificationsBySender(getItem(position).getPerson().getUuid());
            notificationByReceiver = notificationController.getAllNotificationsByReceiver(getItem(position).getPerson().getUuid());
            allMessagesThreadForPerson.addAll(notificationBySender);
            allMessagesThreadForPerson.addAll(notificationByReceiver);
        } catch (NotificationController.NotificationFetchException e) {
            e.printStackTrace( );
        } catch (ParseException e) {
            e.printStackTrace( );
        }
        return convertView;
    }


    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }


    public class ViewHolder {
        private TextView subject;
        private TextView notificationDate;
        private String status;
        private ImageView newNotificationImg;

        public ViewHolder(View convertView) {
            this.subject = (TextView) convertView.findViewById(R.id.sender_textview);
            this.notificationDate = (TextView) convertView.findViewById(R.id.provider_identifier);
            this.newNotificationImg = (ImageView) convertView.findViewById(R.id.provider_circle_imageview);
        }

        public void setName(String text) {
            subject.setText(text);
        }

        public void setId(String id) {
            notificationDate.setText(id);
        }

        public void setIdentifier(String status) {
            this.status = status;
        }

        public void markUnreadNotification() {

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
