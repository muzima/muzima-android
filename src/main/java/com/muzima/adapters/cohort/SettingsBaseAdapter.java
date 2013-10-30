package com.muzima.adapters.cohort;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            Set<String> stringSet = cohortPrefixPref.getStringSet(prefixPrefKey, new HashSet<String>());
            addAll(stringSet);
        } else {
            // TODO: Extra this custom implementation of getStringSet into util function
            int index = 1;
            String cohortPrefix = cohortPrefixPref.getString(prefixPrefKey + index, null);
            while (cohortPrefix != null){
                add(cohortPrefix);
                index++;
                cohortPrefix = cohortPrefixPref.getString(prefixPrefKey + index, null);
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_preference, parent, false);
            holder = new ViewHolder(convertView, position);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.setTextForTextField(getItem(position));
        return convertView;
    }

    protected class ViewHolder {
        private TextView text;
        private ImageButton deleteButton;

        public ViewHolder(View convertView, int position) {
            text = (TextView) convertView.findViewById(R.id.prefix);
            text.setOnClickListener(new OnPrefChangeListener(position));
            deleteButton = (ImageButton) convertView.findViewById(R.id.del_cohort_prefix_btn);
            deleteButton.setOnClickListener(new OnPrefDeleteListener(position));
        }

        public void setTextForTextField(String textToBeDisplayed) {
            text.setText(textToBeDisplayed);
        }
    }

    private class OnPrefChangeListener implements View.OnClickListener {
        private int position;

        OnPrefChangeListener(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View view) {
            if (preferenceClickListener != null) {
                preferenceClickListener.onChangePreferenceClick(getItem(position));
            }
        }
    }

    private class OnPrefDeleteListener implements View.OnClickListener {
        private int position;

        OnPrefDeleteListener(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View view) {
            if (preferenceClickListener != null) {
                preferenceClickListener.onDeletePreferenceClick(getItem(position));
            }
        }
    }
}
