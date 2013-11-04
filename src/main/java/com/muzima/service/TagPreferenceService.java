package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.muzima.utils.Constants.FORM_TAG_PREF;
import static com.muzima.utils.Constants.FORM_TAG_PREF_KEY;

public class TagPreferenceService extends PreferenceService {

    public TagPreferenceService(Context context) {
        super(context);
    }

    public void saveSelectedTags(Set<String> selectedTags) {
        SharedPreferences cohortSharedPref = context.getSharedPreferences(FORM_TAG_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = cohortSharedPref.edit();
        putStringSet(FORM_TAG_PREF_KEY, selectedTags, editor);
    }

    public List<String> getSelectedTags(){
        SharedPreferences cohortSharedPref = context.getSharedPreferences(FORM_TAG_PREF, MODE_PRIVATE);
        return deserialize(cohortSharedPref.getString(FORM_TAG_PREF_KEY, null));
    }
}
