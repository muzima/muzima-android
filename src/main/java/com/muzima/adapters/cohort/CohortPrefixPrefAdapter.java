/**
 * Copyright 2012 Muzima Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
