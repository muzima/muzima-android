/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.adapters.cohort;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.service.CohortPrefixPreferenceService;
import com.muzima.view.preferences.CohortPreferenceActivity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Responsible to display CohortPrefixes in the CohortPreferenceActivity
 */
public class CohortPrefixPrefAdapter extends ListAdapter<String> {

    private final CohortPrefixPreferenceService cohortPrefixPreferenceService;
    private final CohortPreferenceActivity cohortPreferenceActivity;

    public CohortPrefixPrefAdapter(Context context, int textViewResourceId, CohortPreferenceActivity cohortPreferenceActivity) {
        super(context, textViewResourceId);
        this.cohortPreferenceActivity = cohortPreferenceActivity;
        cohortPrefixPreferenceService = new CohortPrefixPreferenceService(context);
        reloadData();
    }

    @Override
    public void reloadData() {
        clear();
        List<String> cohortPrefixes = cohortPrefixPreferenceService.getCohortPrefixes();
        Collections.sort(cohortPrefixes, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return lhs.toLowerCase().compareTo(rhs.toLowerCase());
            }
        });
        addAll(cohortPrefixes);
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
            deleteButton = (ImageButton) convertView.findViewById(R.id.del_cohort_prefix_btn);
            deleteButton.setOnClickListener(new OnPrefDeleteListener(position));
        }

        public void setTextForTextField(String textToBeDisplayed) {
            text.setText(textToBeDisplayed);
        }
    }
    private class OnPrefDeleteListener implements View.OnClickListener {
        private int position;

        private OnPrefDeleteListener(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View view) {
            cohortPreferenceActivity.onDeletePreferenceClick(getItem(position));
        }
    }
}
