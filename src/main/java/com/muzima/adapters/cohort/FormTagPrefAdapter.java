package com.muzima.adapters.cohort;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;

import java.util.HashSet;
import java.util.Set;

import static com.muzima.utils.Constants.*;

public class FormTagPrefAdapter extends ListAdapter<String> {

    private TagClickListener tagClickListener;

    public FormTagPrefAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        reloadData();
    }

    public void setTagClickListener(TagClickListener tagClickListener) {
        this.tagClickListener = tagClickListener;
    }

    @Override
    public void reloadData() {
        clear();
        SharedPreferences formTagPref = getContext().getSharedPreferences(FORM_TAG_PREF, Context.MODE_PRIVATE);
        Set<String> stringSet = formTagPref.getStringSet(FORM_TAG_PREF_KEY, new HashSet<String>());
        addAll(stringSet);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_cohorts_list_pref, parent, false);
            holder = new ViewHolder();
            holder.text = (TextView) convertView
                    .findViewById(R.id.prefix);
            holder.text.setOnClickListener(new OnTagChangeListener(position));

            holder.deleteButton = (FrameLayout)convertView.findViewById(R.id.delete_button);
            holder.deleteButton.setOnClickListener(new OnTagDeleteListener(position));
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.text.setText(getItem(position));
        return convertView;
    }

    protected static class ViewHolder {
        TextView text;
        FrameLayout deleteButton;
    }

    public static interface TagClickListener {
        public void onDeleteTagClick(String tag);
        public void onChangeTagClick(String tag);
    }

    private class OnTagChangeListener implements View.OnClickListener {
        private int position;
        OnTagChangeListener(int position){
            this.position = position;
        }
        @Override
        public void onClick(View view) {
            if(tagClickListener != null){
                tagClickListener.onChangeTagClick(getItem(position));
            }
        }
    }

    private class OnTagDeleteListener implements View.OnClickListener {
        private int position;
        OnTagDeleteListener(int position){
            this.position = position;
        }
        @Override
        public void onClick(View view) {
            if(tagClickListener != null){
                tagClickListener.onDeleteTagClick(getItem(position));
            }
        }
    }
}
