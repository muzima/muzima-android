package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;

import com.muzima.api.model.Concept;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.muzima.utils.Constants.CONCEPT_PREF;
import static com.muzima.utils.Constants.CONCEPT_PREF_KEY;

public class ConceptPreferenceService extends PreferenceService {

    private final SharedPreferences conceptSharedPreferences;

    public ConceptPreferenceService(Context context) {
        super(context);
        conceptSharedPreferences = context.getSharedPreferences(CONCEPT_PREF, MODE_PRIVATE);
    }

    public void addConcepts(List<Concept> concepts) {
        Set<String> copyOfUuidSet = new LinkedHashSet<String>(getConcepts());
        for (Concept concept : concepts) {
            copyOfUuidSet.add(concept.getUuid());
        }
        saveConcepts(copyOfUuidSet);
    }

    public void removeConcept(Concept concept) {
        Set<String> concepts = new HashSet<String>(getConcepts());
        concepts.remove(concept.getUuid());
        saveConcepts(concepts);
    }

    public List<String> getConcepts(){
        return deserialize(conceptSharedPreferences.getString(CONCEPT_PREF_KEY, null));
    }

    private void saveConcepts(Set<String> copyOfUuidSet) {
        SharedPreferences.Editor editor = conceptSharedPreferences.edit();
        putStringSet(CONCEPT_PREF_KEY, copyOfUuidSet, editor);
        editor.commit();
    }

    public void clearConcepts() {
        saveConcepts(null);
    }
}
