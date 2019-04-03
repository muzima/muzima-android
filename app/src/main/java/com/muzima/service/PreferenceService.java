/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;

import com.muzima.utils.StringUtils;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

abstract class PreferenceService {
    final Context context;

    PreferenceService(Context context) {
        this.context = context;
    }

    String serialize(Collection<String> values) {
        if(values == null){
            return null;
        }
        JSONArray jsonArray = new JSONArray();
        for (String cohort : values) {
            jsonArray.put(cohort);
        }
        return jsonArray.toString();
    }

    List<String> deserialize(String json) {
        if (StringUtils.isEmpty(json))
            return new ArrayList<>();

        List<String> cohortsList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                cohortsList.add(jsonArray.get(i).toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cohortsList;
    }

    void putStringSet(Set<String> values, SharedPreferences.Editor editor) {
        editor.putString(com.muzima.utils.Constants.FORM_TAG_PREF_KEY, serialize(values));
        editor.commit();
    }
}
