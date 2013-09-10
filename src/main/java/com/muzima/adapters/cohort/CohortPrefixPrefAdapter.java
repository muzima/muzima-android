package com.muzima.adapters.cohort;

import android.content.Context;

import static com.muzima.utils.Constants.COHORT_PREFIX_PREF;
import static com.muzima.utils.Constants.COHORT_PREFIX_PREF_KEY;

public class CohortPrefixPrefAdapter extends SettingsBaseAdapter {

    public CohortPrefixPrefAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        prefixPref = COHORT_PREFIX_PREF;
        prefixPrefKey = COHORT_PREFIX_PREF_KEY;
        reloadData();
    }
}
