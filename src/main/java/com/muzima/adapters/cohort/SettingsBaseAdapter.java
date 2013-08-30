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

public abstract class SettingsBaseAdapter extends ListAdapter<String> {
    private PreferenceClickListener preferenceClickListener;
    protected String prefixPref;
    protected String prefixPrefKey;

    public SettingsBaseAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public void setPreferenceClickListener(PreferenceClickListener preferenceClickListener) {
        this.preferenceClickListener = preferenceClickListener;
    }

    @Override
    public void reloadData() {
        clear();
        SharedPreferences cohortPrefixPref = getContext().getSharedPreferences(prefixPref, Context.MODE_PRIVATE);
        Set<String> stringSet = cohortPrefixPref.getStringSet(prefixPrefKey, new HashSet<String>());
        addAll(stringSet);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_preference, parent, false);
            holder = new ViewHolder();
            holder.text = (TextView) convertView
                    .findViewById(R.id.prefix);
            holder.text.setOnClickListener(new OnPrefChangeListener(position));

            holder.deleteButton = (FrameLayout)convertView.findViewById(R.id.delete_button);
            holder.deleteButton.setOnClickListener(new OnPrefDeleteListener(position));
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.text.setText(getItem(position));
        return convertView;
    }

    public static interface PreferenceClickListener {
        public void onDeletePreferenceClick(String pref);
        public void onChangePreferenceClick(String pref);
    }

    protected static class ViewHolder {
        TextView text;
        FrameLayout deleteButton;
    }

    private class OnPrefChangeListener implements View.OnClickListener {
        private int position;
        OnPrefChangeListener(int position){
            this.position = position;
        }
        @Override
        public void onClick(View view) {
            if(preferenceClickListener != null){
                preferenceClickListener.onChangePreferenceClick(getItem(position));
            }
        }
    }

    private class OnPrefDeleteListener implements View.OnClickListener {
        private int position;
        OnPrefDeleteListener(int position){
            this.position = position;
        }
        @Override
        public void onClick(View view) {
            if(preferenceClickListener != null){
                preferenceClickListener.onDeletePreferenceClick(getItem(position));
            }
        }
    }
}
