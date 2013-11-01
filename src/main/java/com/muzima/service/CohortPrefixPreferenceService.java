package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static com.muzima.utils.Constants.COHORT_PREFIX_PREF;
import static com.muzima.utils.Constants.COHORT_PREFIX_PREF_KEY;

public class CohortPrefixPreferenceService extends PreferenceService {

    public CohortPrefixPreferenceService(Context context) {
        super(context);
    }

    public List<String> getCohortPrefixes() {
        SharedPreferences cohortSharedPref = context.getSharedPreferences(COHORT_PREFIX_PREF, MODE_PRIVATE);
        return deserialize(cohortSharedPref.getString(COHORT_PREFIX_PREF_KEY, ""));
    }

    public boolean addCohortPrefix(String newPrefix) {
        SharedPreferences cohortSharedPref = context.getSharedPreferences(COHORT_PREFIX_PREF, MODE_PRIVATE);
        List<String> copiedPrefixesSet = new ArrayList<String>(getCohortPrefixes(cohortSharedPref));

        if (!copiedPrefixesSet.contains(newPrefix)) {
            copiedPrefixesSet.add(newPrefix);
            saveCohortPrefixes(cohortSharedPref, copiedPrefixesSet);
            return true;
        }
        return false;
    }

    public void deleteCohortPrefix(String pref) {
        SharedPreferences cohortSharedPref = context.getSharedPreferences(COHORT_PREFIX_PREF, MODE_PRIVATE);
        List<String> prefixes = getCohortPrefixes(cohortSharedPref);
        prefixes.remove(pref);
        saveCohortPrefixes(cohortSharedPref, prefixes);
    }

    private void saveCohortPrefixes(SharedPreferences cohortSharedPref, List<String> prefixes) {
        SharedPreferences.Editor editor = cohortSharedPref.edit();
        editor.putString(COHORT_PREFIX_PREF_KEY, serialize(prefixes));
        editor.commit();
    }

    private List<String> getCohortPrefixes(SharedPreferences cohortSharedPref) {
        return deserialize(cohortSharedPref.getString(COHORT_PREFIX_PREF_KEY, null));
    }
}
