/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.adapters.forms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Tag;
import com.muzima.controller.FormController;
import com.muzima.model.BaseForm;
import com.muzima.utils.Fonts;
import com.muzima.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible to display forms in list. This adapter displays form names on the list. You can identify the forms by form names.
 * @param <T> T is of type AvailableForm, FormWithData.
 */
public abstract class FormsAdapter<T extends BaseForm> extends ListAdapter<T> {
    private static final String TAG = "FormsAdapter";
    protected FormController formController;
    protected BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    public FormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId);
        this.formController = formController;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    getFormItemLayout(), parent, false);
            holder = new ViewHolder();
            holder.name = (CheckedTextView) convertView.findViewById(R.id.form_name);
            holder.description = (TextView) convertView.findViewById(R.id.form_description);
            holder.savedTime = (TextView) convertView.findViewById(R.id.form_save_time);
            holder.encounterDate = (TextView) convertView.findViewById(R.id.form_encounter_date);
            holder.tagsScroller = (RelativeLayout) convertView.findViewById(R.id.tags_scroller);
            holder.tagsLayout = (LinearLayout) convertView.findViewById(R.id.menu_tags);
            holder.tags = new ArrayList<TextView>();
            holder.downloadedImg = (ImageView) convertView.findViewById(R.id.downloadImg);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (!isEmpty()) {
            BaseForm form = getItem(position);

            holder.name.setText(form.getName());
            holder.name.setTypeface(Fonts.roboto_medium(getContext()));

            String description = form.getDescription();
            if (StringUtils.isEmpty(description)) {
                description = "No description available";
            }
            holder.description.setText(description);
            holder.description.setTypeface(Fonts.roboto_light(getContext()));
            holder.savedTime.setVisibility(View.GONE);
        }
        return convertView;
    }

    protected int getFormItemLayout() {
        return R.layout.item_forms_list;
    }

    protected List<String> getSelectedTagUuids() {
        List<Tag> selectedTags = formController.getSelectedTags();
        List<String> tags = new ArrayList<String>();
        for (Tag selectedTag : selectedTags) {
            tags.add(selectedTag.getUuid());
        }
        return tags;
    }

    protected static class ViewHolder {
        CheckedTextView name;
        ImageView downloadedImg;
        TextView description;
        RelativeLayout tagsScroller;
        LinearLayout tagsLayout;
        List<TextView> tags;
        TextView savedTime;
        TextView encounterDate;

        public void addTag(TextView tag) {
            this.tags.add(tag);
            tagsLayout.addView(tag);
        }

        public void removeTags(List<TextView> tagsToRemove) {
            for (TextView tag : tagsToRemove) {
                tagsLayout.removeView(tag);
            }
            tags.removeAll(tagsToRemove);
        }
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public BackgroundListQueryTaskListener getBackgroundListQueryTaskListener() {
        return backgroundListQueryTaskListener;
    }

    public FormController getFormController() {
        return formController;
    }

    public interface MuzimaClickListener {

        boolean onItemLongClick();

        void onItemClick(int position);
    }

}
