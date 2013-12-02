package com.muzima.adapters.cohort;

import android.content.Context;
import com.muzima.service.CohortPrefixPreferenceService;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Responsible to display CohortPrefixes in the CohortPreferenceActivity
 */
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
        List<String> cohortPrefixes = cohortPrefixPreferenceService.getCohortPrefixes();
        Collections.sort(cohortPrefixes, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return lhs.toLowerCase().compareTo(rhs.toLowerCase());
            }
        });
        addAll(cohortPrefixes);
    }
}
