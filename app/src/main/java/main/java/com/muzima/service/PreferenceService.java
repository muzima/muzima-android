/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.service;

import android.content.Context;
import com.muzima.utils.StringUtils;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

abstract class PreferenceService {
    protected Context context;

    public PreferenceService(Context context) {
        this.context = context;
    }

    protected String serialize(Collection<String> values) {
        if(values == null){
            return null;
        }
        JSONArray jsonArray = new JSONArray();
        for (String cohort : values) {
            jsonArray.put(cohort);
        }
        return jsonArray.toString();
    }

    protected List<String> deserialize(String json) {
        if (StringUtils.isEmpty(json))
            return new ArrayList<String>();

        List<String> cohortsList = new ArrayList<String>();
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

    protected void putStringSet(String key, Set<String> values, android.content.SharedPreferences.Editor editor) {
        editor.putString(key, serialize(values));
        editor.commit();
    }
}
