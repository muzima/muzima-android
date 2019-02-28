/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.adapters.concept;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Concept;
import com.muzima.controller.ConceptController;
import com.muzima.view.preferences.ConceptPreferenceActivity;

import java.util.Arrays;
import java.util.List;

/**
 * Responsible to display Concepts in the Settings page.
 */
public class SelectedConceptAdapter extends ListAdapter<Concept> {
    private final ConceptController conceptController;

    public SelectedConceptAdapter(ConceptPreferenceActivity context, int textViewResourceId, ConceptController conceptController) {
        super(context, textViewResourceId);
        this.conceptController = conceptController;
    }

    public boolean doesConceptAlreadyExist(Concept selectedConcept) {
        try {
            return conceptController.getConcepts().contains(selectedConcept);
        } catch (ConceptController.ConceptFetchException e) {
            Log.e(getClass().getSimpleName(), "Error while loading concepts", e);
        }
        return false;
    }

    private class ViewHolder {
        private final CheckedTextView name;
        private final CheckedTextView synonyms;

        private ViewHolder(View conceptView) {
            name = conceptView.findViewById(R.id.concept_name);
            synonyms = conceptView.findViewById(R.id.concept_synonyms);
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_concept_list, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        holder = (ViewHolder) convertView.getTag();
        Concept concept = getItem(position);
        if (concept != null) {
            holder.name.setText(concept.getName());
            holder.synonyms.setText(concept.getSynonyms());
        }
        return convertView;
    }

    @Override
    public void remove(Concept concept) {
        super.remove(concept);
        try {
            conceptController.deleteConcept(concept);
        } catch (ConceptController.ConceptDeleteException e) {
            Log.e(getClass().getSimpleName(), "Error while deleting the concept", e);
        }
    }

    public void removeAll(List<Concept> conceptsToDelete) {
        try {
            List<Concept> allConcepts = conceptController.getConcepts();
            allConcepts.removeAll(conceptsToDelete);
            try {
                conceptController.deleteConcepts(conceptsToDelete);
            } catch (ConceptController.ConceptDeleteException e) {
                Log.e(getClass().getSimpleName(), "Error while deleting the concept", e);
            }
            this.clear();
            this.addAll(allConcepts);
        } catch (ConceptController.ConceptFetchException e) {
            Log.e(getClass().getSimpleName(), "Error while fetching the concept", e);
        }
    }

    @Override
    public void reloadData() {
        new BackgroundSaveAndQueryTask().execute();
    }

    /**
     * Responsible to save the concept into DB on selection from AutoComplete. And also fetches to Concepts from DB to display in the page.
     */
    class BackgroundSaveAndQueryTask extends AsyncTask<Concept, Void, List<Concept>> {

        @Override
        protected List<Concept> doInBackground(Concept... concepts) {
            List<Concept> selectedConcepts = null;
            List<Concept> conceptList = Arrays.asList(concepts);
            try {
                if (concepts.length > 0) {
                    // Called with Concept which is selected in the AutoComplete menu.
                    conceptController.saveConcepts(conceptList);
                }
                if(conceptController.newConcepts().size() > 0){
                    // called when new concepts are downloaded as part of new form template
                    return conceptController.newConcepts();
                }
                selectedConcepts = conceptController.getConcepts();
            } catch (ConceptController.ConceptSaveException e) {
                Log.w(getClass().getSimpleName(), "Exception occurred while saving concept to local data repository!", e);
            } catch (ConceptController.ConceptFetchException e) {
                Log.w(getClass().getSimpleName(), "Exception occurred while fetching concepts from local data repository!", e);
            }
            return selectedConcepts;
        }

        @Override
        protected void onPostExecute(List<Concept> concepts) {
            if (concepts == null) {
                Toast.makeText(getContext(), getContext().getString(R.string.error_concept_fetch), Toast.LENGTH_SHORT).show();
                return;
            }
            clear();
            addAll(concepts);
            notifyDataSetChanged();
        }
    }

    public void addConcept(Concept concept) {
        new BackgroundSaveAndQueryTask().execute(concept);
    }

    public void clearSelectedForms() {
        notifyDataSetChanged();
    }
}
