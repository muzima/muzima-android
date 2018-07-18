package com.muzima.adapters.providers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Provider;
import com.muzima.controller.ProviderController;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.Fonts;
import com.muzima.utils.StringUtils;

import java.util.Date;

public abstract class ProvidersAdapter extends ListAdapter<Provider> {

    protected ProviderController providerController;
    protected BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    public ProvidersAdapter(Context context, int textViewResourceId, ProviderController providerController) {
        super(context, textViewResourceId);
        this.providerController = providerController;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ProvidersAdapter.ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_providers_list, parent, false);
            holder = new ProvidersAdapter.ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ProvidersAdapter.ViewHolder) convertView.getTag();
        }
        holder.setIdentifier(getItem(position).getIdentifier());
        holder.setName(getItem(position).getName());
        holder.setId(getItem(position).getId());
        holder.markUnreadNotification();
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

        public void setId(Integer id) {
            notificationDate.setText(Integer.toString(id));
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
