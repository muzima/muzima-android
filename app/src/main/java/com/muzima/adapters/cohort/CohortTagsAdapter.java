/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.cohort;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.muzima.R;

import java.util.List;

public class CohortTagsAdapter extends ArrayAdapter<String> {

    private List<String> tags;
    private Context context;
    public CohortTagsAdapter(@NonNull Context context, int resource, List<String> tags) {
        super(context, resource);
        this.tags = tags;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.item_cohort_tags_layout,null);
        TextView titleTextView = view.findViewById(R.id.item_cohort_tag_text_view);
        titleTextView.setText(tags.get(position));
        return view;
    }

    @Override
    public int getCount() {
        return tags.size();
    }
}
