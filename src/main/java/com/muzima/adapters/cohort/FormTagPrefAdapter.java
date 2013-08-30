package com.muzima.adapters.cohort;

import android.content.Context;

import static com.muzima.utils.Constants.FORM_TAG_PREF;
import static com.muzima.utils.Constants.FORM_TAG_PREF_KEY;

public class FormTagPrefAdapter extends SettingsBaseAdapter {

    public FormTagPrefAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);

        prefixPref = FORM_TAG_PREF;
        prefixPrefKey = FORM_TAG_PREF_KEY;
        reloadData();
    }
}
