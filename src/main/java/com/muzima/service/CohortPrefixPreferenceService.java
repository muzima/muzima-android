package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static com.muzima.utils.Constants.COHORT_PREFIX_PREF;
import static com.muzima.utils.Constants.COHORT_PREFIX_PREF_KEY;

public class CohortPrefixPreferenceService extends PreferenceService {

    private final SharedPreferences cohortSharedPref;

    public CohortPrefixPreferenceService(Context context) {
        super(context);
        cohortSharedPref = context.getSharedPreferences(COHORT_PREFIX_PREF, MODE_PRIVATE);
    }

    public List<String> getCohortPrefixes() {
        return deserialize(cohortSharedPref.getString(COHORT_PREFIX_PREF_KEY, ""));
    }

    public boolean addCohortPrefix(String newPrefix) {
        List<String> copiedPrefixesSet = new ArrayList<String>(getCohortPrefixes(cohortSharedPref));

        if (!copiedPrefixesSet.contains(newPrefix)) {
            copiedPrefixesSet.add(newPrefix);
            saveCohortPrefixes(copiedPrefixesSet);
            return true;
        }
        return false;
    }

    public void deleteCohortPrefix(String pref) {
        List<String> prefixes = getCohortPrefixes(cohortSharedPref);
        prefixes.remove(pref);
        saveCohortPrefixes(prefixes);
    }

    private void saveCohortPrefixes( List<String> prefixes) {
        SharedPreferences.Editor editor = cohortSharedPref.edit();
        editor.putString(COHORT_PREFIX_PREF_KEY, serialize(prefixes));
        editor.commit();
    }

    private List<String> getCohortPrefixes(SharedPreferences cohortSharedPref) {
        return deserialize(cohortSharedPref.getString(COHORT_PREFIX_PREF_KEY, null));
    }

    public void clearPrefixes() {
        saveCohortPrefixes(null);
    }
}
