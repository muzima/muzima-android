package com.muzima.adapters.providers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Provider;
import com.muzima.controller.NotificationController;
import com.muzima.controller.ProviderController;
import com.muzima.utils.Constants;
import com.muzima.utils.Fonts;
import com.muzima.utils.StringUtils;

import org.apache.lucene.queryParser.ParseException;

import static com.muzima.utils.Constants.NotificationStatusConstants.NOTIFICATION_UNREAD;

public abstract class ProvidersAdapter extends ListAdapter<Provider> {

    private final NotificationController notificationController;
    protected BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    protected ProvidersAdapter(Context context, int textViewResourceId, MuzimaApplication muzimaApplication) {
        super(context, textViewResourceId);
        ProviderController providerController = muzimaApplication.getProviderController();
        this.notificationController = muzimaApplication.getNotificationController();
        MuzimaApplication muzimaApplication1 = muzimaApplication;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
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
        } catch (NotificationController.NotificationFetchException | ParseException e) {
            Log.e(getClass().getSimpleName(),e.getMessage());
        }
        holder.setName(getItem(position).getName( ));
        holder.markUnreadNotification( );
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
            this.subject = convertView.findViewById(R.id.sender_textview);
            this.notificationDate = convertView.findViewById(R.id.provider_identifier);
            this.newNotificationImg = convertView.findViewById(R.id.provider_circle_imageview);
        }

        void setName(String text) {
            subject.setText(text);
        }

        void setId(String id) {
            notificationDate.setText(id);
        }

        void setIdentifier(String status) {
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
