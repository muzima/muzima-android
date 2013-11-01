package com.muzima.adapters.cohort;

import android.content.Context;
import com.muzima.service.CohortPrefixPreferenceService;

public class CohortPrefixPrefAdapter extends SettingsBaseAdapter {

    private final CohortPrefixPreferenceService cohortPrefixPreferenceService;

    public CohortPrefixPrefAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        cohortPrefixPreferenceService = new CohortPrefixPreferenceService(context);
        reloadData();
    }

    @Override
    public void reloadData() {
        clear();
        addAll(cohortPrefixPreferenceService.getCohortPrefixes());
    }
}
