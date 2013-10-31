package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.muzima.api.model.Concept;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
            int index = 1;
            while (conceptSharedPreferences.getString(CONCEPT_PREF_KEY + index, null) != null) {
                index++;
            }
            SharedPreferences.Editor editor = conceptSharedPreferences.edit();
            for (Concept concept : concepts) {
                editor.putString(CONCEPT_PREF_KEY + index, concept.getUuid());
                index++;
            }
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
        List<String> cohortsList = new ArrayList<String>();
        try {
            JSONArray jsonArray = new JSONArray(sharedPreferenceString);
            for (int i = 0; i < jsonArray.length(); i++) {
                cohortsList.add(jsonArray.get(i).toString());
            }
        } catch (Exception e) {
        }
        return cohortsList;
    }
}
