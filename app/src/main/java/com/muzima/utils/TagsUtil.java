/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientTag;

public class TagsUtil {

    public static void loadTags (Patient patient, ViewGroup tagsLayout, Context context) {
        PatientTag[] tags = patient.getTags();
        if (tags != null && tags.length > 0) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);

            for (int i = 0; i < tags.length; i++) {
                TextView tag =  newTextView(layoutInflater);
                tag.setBackgroundColor(((MuzimaApplication)context).getPatientController().getTagColor(tags[i].getUuid()));
                tag.setText(tags[i].getName());
                tagsLayout.addView(tag);
            }
        }
    }

    private static TextView newTextView(LayoutInflater layoutInflater) {
        TextView textView = (TextView) layoutInflater.inflate(R.layout.tag, null, false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(1, 0, 0, 0);
        textView.setLayoutParams(layoutParams);
        return textView;
    }
}
