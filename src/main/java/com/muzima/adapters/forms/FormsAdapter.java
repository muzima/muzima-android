package com.muzima.adapters.forms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Form;
import com.muzima.api.model.Tag;
import com.muzima.controller.FormController;
import com.muzima.search.api.util.StringUtil;
import com.muzima.utils.Fonts;
import com.muzima.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class FormsAdapter extends ListAdapter<Form> {
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
                    R.layout.item_forms_list, parent, false);
            holder = new ViewHolder();
            holder.name = (TextView) convertView
                    .findViewById(R.id.form_name);
            holder.description = (TextView) convertView
                    .findViewById(R.id.form_description);
            holder.tagsScroller = (HorizontalScrollView) convertView.findViewById(R.id.tags_scroller);
            holder.tagsLayout = (LinearLayout) convertView.findViewById(R.id.menu_tags);
            holder.tags = new ArrayList<TextView>();
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Form form = getItem(position);

        holder.name.setText(form.getName());
        holder.name.setTypeface(Fonts.roboto_medium(getContext()));

        String description = form.getDescription();
        if (StringUtils.isEmpty(description)) {
            description = "No description available";
        }
        holder.description.setText(description);
        holder.description.setTypeface(Fonts.roboto_light(getContext()));

        return convertView;
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
        TextView name;
        TextView description;
        HorizontalScrollView tagsScroller;
        LinearLayout tagsLayout;
        List<TextView> tags;

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
}
