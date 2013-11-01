package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import com.muzima.api.model.Concept;
import com.muzima.utils.Constants;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.muzima.utils.Constants.CONCEPT_PREF;
import static com.muzima.utils.Constants.CONCEPT_PREF_KEY;

public class ConceptPreferenceService extends PreferenceService {

    public ConceptPreferenceService(Context context) {
        super(context);
    }

    public void addConcepts(List<Concept> concepts) {
        SharedPreferences conceptSharedPreferences = context.getSharedPreferences(CONCEPT_PREF, MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
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
            List<String> conceptUuidSet = deserialize(conceptSharedPreferences.getString(CONCEPT_PREF_KEY, null));
            SharedPreferences.Editor editor = conceptSharedPreferences.edit();
            Set<String> copyOfUuidSet = new LinkedHashSet<String>();
            copyOfUuidSet.addAll(conceptUuidSet);
            for (Concept concept : concepts) {
                copyOfUuidSet.add(concept.getUuid());
            }
            putStringSet(CONCEPT_PREF_KEY, copyOfUuidSet, editor);
            editor.commit();
        }
    }

    public void removeConcept(Concept concept) {
        SharedPreferences conceptSharedPreferences = context.getSharedPreferences(CONCEPT_PREF, MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
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

    public List<String> getConcepts(){
        SharedPreferences cohortSharedPref = context.getSharedPreferences(Constants.CONCEPT_PREF, android.content.Context.MODE_PRIVATE);
        return deserialize(cohortSharedPref.getString(CONCEPT_PREF_KEY, null));
    }
}
