package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.muzima.utils.Constants.FORM_TAG_PREF;
import static com.muzima.utils.Constants.FORM_TAG_PREF_KEY;

public class TagPreferenceService extends PreferenceService {

    private SharedPreferences tagSharedPreferences;

    public TagPreferenceService(Context context) {
        super(context);
        tagSharedPreferences = context.getSharedPreferences(FORM_TAG_PREF, MODE_PRIVATE);
    }

    public void saveSelectedTags(Set<String> selectedTags) {
        SharedPreferences.Editor editor = tagSharedPreferences.edit();
        putStringSet(FORM_TAG_PREF_KEY, selectedTags, editor);
    }

    public List<String> getSelectedTags(){
        return deserialize(tagSharedPreferences.getString(FORM_TAG_PREF_KEY, null));
    }
}
