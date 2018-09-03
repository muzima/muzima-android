/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.muzima.utils.Constants.FORM_TAG_PREF;
import static com.muzima.utils.Constants.FORM_TAG_PREF_KEY;

public class TagPreferenceService extends PreferenceService {

    private final SharedPreferences tagSharedPreferences;

    public TagPreferenceService(Context context) {
        super(context);
        tagSharedPreferences = context.getSharedPreferences(FORM_TAG_PREF, MODE_PRIVATE);
    }

    public void saveSelectedTags(Set<String> selectedTags) {
        SharedPreferences.Editor editor = tagSharedPreferences.edit();
        putStringSet(selectedTags, editor);
    }

    public List<String> getSelectedTags(){
        return deserialize(tagSharedPreferences.getString(FORM_TAG_PREF_KEY, null));
    }
}
