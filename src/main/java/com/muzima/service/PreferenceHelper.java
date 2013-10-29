package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.muzima.api.model.Concept;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
            while (conceptSharedPreferences.getString(CONCEPT_PREF_KEY+index, null)!= null){
                index++;
            }
            SharedPreferences.Editor editor = conceptSharedPreferences.edit();
            for (Concept concept : concepts) {
                editor.putString(CONCEPT_PREF_KEY+index, concept.getUuid());
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
}
