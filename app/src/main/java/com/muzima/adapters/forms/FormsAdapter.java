/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.adapters.forms;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.model.BaseForm;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible to display forms in list. This adapter displays form names on the list. You can identify the forms by form names.
 * @param <T> T is of type AvailableForm, FormWithData.
 */
public abstract class FormsAdapter<T extends BaseForm> extends ListAdapter<T> {
    final FormController formController;
    final ObservationController observationController;
    protected BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    private MuzimaAsyncTask<?, ?, ?> backgroundQueryTask;
    private Context context;

    protected FormsAdapter(Context context, int textViewResourceId, FormController formController, ObservationController observationController) {
        super(context, textViewResourceId);
        this.formController = formController;
        this.observationController = observationController;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    getFormItemLayout(), parent, false);
            holder = new ViewHolder();
            holder.name = convertView.findViewById(R.id.form_name);
            holder.description = convertView.findViewById(R.id.form_description);
            holder.savedTime = convertView.findViewById(R.id.form_save_time);
            holder.encounterDate = convertView.findViewById(R.id.form_encounter_date);
            holder.tagsScroller = convertView.findViewById(R.id.tags_scroller);
            holder.tagsLayout = convertView.findViewById(R.id.menu_tags);
            holder.tags = new ArrayList<>();
            holder.downloadedImg = convertView.findViewById(R.id.downloadImg);
            holder.updateAvailableImg = convertView.findViewById(R.id.downloadImg);

            convertView.setTag(holder);
            setIconImageSize(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (!isEmpty()) {
            BaseForm form = getItem(position);

            holder.name.setText(form.getName());

            String description = form.getDescription();
            if (StringUtils.isEmpty(description)) {
                description = getContext().getString(R.string.general_description_unavailable);
            }
            holder.description.setText(description);
            holder.savedTime.setVisibility(View.GONE);
        }
        return convertView;
    }

    private void setIconImageSize(ViewHolder holder){
        int lineHeight = holder.name.getLineHeight();
        int imageLayoutMargin = 10;
        int imageSize = 2*lineHeight-2*imageLayoutMargin;
        int width = imageSize;
        int height = imageSize;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width,height);
        params.setMargins(imageLayoutMargin,imageLayoutMargin,imageLayoutMargin,imageLayoutMargin);
        holder.downloadedImg.setLayoutParams(params);
        holder.updateAvailableImg.setLayoutParams(params);
    }

    int getFormItemLayout() {
        return R.layout.item_forms_list;
    }


    static class ViewHolder {
        CheckedTextView name;
        ImageView downloadedImg;
        ImageView updateAvailableImg;
        TextView description;
        RelativeLayout tagsScroller;
        LinearLayout tagsLayout;
        List<TextView> tags;
        TextView savedTime;
        TextView encounterDate;
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public BackgroundListQueryTaskListener getBackgroundListQueryTaskListener() {
        return backgroundListQueryTaskListener;
    }

    public void cancelBackgroundQueryTask() {
        if (backgroundQueryTask != null) {
            backgroundQueryTask.cancel();
        }
    }

    public FormController getFormController() {
        return ((MuzimaApplication) context.getApplicationContext()).getFormController();
    }

    public interface MuzimaClickListener {

        void onItemLongClick();

        void onItemClick(int position, View view);
    }

}
