package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.muzima.api.model.Concept;

import com.muzima.utils.PreAndroidHoneycomb;
import org.json.JSONArray;

import java.util.*;

import static com.muzima.utils.Constants.COHORT_PREFIX_PREF;
import static com.muzima.utils.Constants.COHORT_PREFIX_PREF_KEY;
import static com.muzima.utils.Constants.CONCEPT_PREF;
import static com.muzima.utils.Constants.CONCEPT_PREF_KEY;

public class PreferenceHelper {
    private Context context;

    public PreferenceHelper(Context context) {
        this.context = context;
    }

    public void addConcepts(List<Concept> concepts) {
        SharedPreferences conceptSharedPreferences = context.getSharedPreferences(CONCEPT_PREF, Context.MODE_PRIVATE);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            Set<String> conceptUuidSet = conceptSharedPreferences.getStringSet(CONCEPT_PREF_KEY, new LinkedHashSet<String>());
            SharedPreferences.Editor editor = conceptSharedPreferences.edit();
            Set<String> copyOfUuidSet = new LinkedHashSet<String>();
            copyOfUuidSet.addAll(conceptUuidSet);
            for (Concept concept : concepts) {
                copyOfUuidSet.add(concept.getUuid());
            }
            editor.putStringSet(CONCEPT_PREF_KEY, copyOfUuidSet);
            editor.commit();
        } else {
//            Set<String> conceptUuidSet = PreAndroidHoneycomb.SharedPreferences.getStringSet(CONCEPT_PREF_KEY, new LinkedHashSet<String>(),conceptSharedPreferences);
            Set<String> conceptUuidSet = getStringSet(CONCEPT_PREF_KEY, new LinkedHashSet<String>(), conceptSharedPreferences);
            SharedPreferences.Editor editor = conceptSharedPreferences.edit();
            Set<String> copyOfUuidSet = new LinkedHashSet<String>();
            copyOfUuidSet.addAll(conceptUuidSet);
            for (Concept concept : concepts) {
                copyOfUuidSet.add(concept.getUuid());
            }
            PreAndroidHoneycomb.SharedPreferences.putStringSet(CONCEPT_PREF_KEY, copyOfUuidSet, editor);
            editor.commit();
        }
    }

    public void removeConcept(Concept concept) {
        SharedPreferences conceptSharedPreferences = context.getSharedPreferences(CONCEPT_PREF, Context.MODE_PRIVATE);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            Set<String> conceptUuidSet = conceptSharedPreferences.getStringSet(CONCEPT_PREF_KEY, new LinkedHashSet<String>());
            SharedPreferences.Editor editor = conceptSharedPreferences.edit();
            Set<String> copyOfUuidSet = new LinkedHashSet<String>();
            copyOfUuidSet.addAll(conceptUuidSet);

            copyOfUuidSet.remove(concept.getUuid());
            editor.putStringSet(CONCEPT_PREF_KEY, copyOfUuidSet);
            editor.commit();
        } else {
//            TODO for FROYO
        }
    }

    public List<String> getCohortPrefixes() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            SharedPreferences cohortSharedPref = context.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE);
            Set<String> cohortSharedPrefStringSet = cohortSharedPref.getStringSet(COHORT_PREFIX_PREF_KEY, new LinkedHashSet<String>());
            return new ArrayList<String>(cohortSharedPrefStringSet);
        } else {
            SharedPreferences cohortSharedPref = context.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE);
            return getStringSet(cohortSharedPref.getString(COHORT_PREFIX_PREF_KEY, ""));
        }
    }

    public void putCohortPrefixes(List<String> cohortPrefixes) {
        SharedPreferences cohortSharedPref = context.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE);
        JSONArray jsonArray = new JSONArray();
        for (String cohort : cohortPrefixes) {
            jsonArray.put(cohort);
        }
        SharedPreferences.Editor editor = cohortSharedPref.edit();
        editor.putString(COHORT_PREFIX_PREF_KEY, jsonArray.toString());
        editor.commit();
    }

    private List<String> getStringSet(String sharedPreferenceString) {
        if (sharedPreferenceString == null)
            return new ArrayList<String>();

        List<String> cohortsList = new ArrayList<String>();
        try {
            JSONArray jsonArray = new JSONArray(sharedPreferenceString);
            for (int i = 0; i < jsonArray.length(); i++) {
                cohortsList.add(jsonArray.get(i).toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cohortsList;
    }
    public Set<String> getStringSet(String key, Set<String> defValues, android.content.SharedPreferences cohortSharedPref){
        // TODO: Implement this properly for FROYO devices
        return new HashSet<String>();
    }
}
