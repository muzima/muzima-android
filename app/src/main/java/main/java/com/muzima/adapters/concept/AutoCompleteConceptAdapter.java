/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.adapters.concept;

import android.content.Context;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import com.muzima.api.model.Concept;
import com.muzima.controller.ConceptController;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible to display and select auto-complete menu of Concept while adding new Concept.
 */

public class AutoCompleteConceptAdapter extends AutoCompleteBaseAdapter<Concept> {
    private static final String TAG = AutoCompleteConceptAdapter.class.getSimpleName();

    public AutoCompleteConceptAdapter(Context context, int textViewResourceId, AutoCompleteTextView autoCompleteConceptTextView) {
        super(context, textViewResourceId, autoCompleteConceptTextView);
    }

    @Override
    protected List<Concept> getOptions(CharSequence constraint) {
        ConceptController conceptController = getMuzimaApplicationContext().getConceptController();
        try {
            return conceptController.downloadConceptsByNamePrefix(constraint.toString());
        } catch (ConceptController.ConceptDownloadException e) {
            Log.e(TAG, "Unable to download concepts!", e);
        }
        return new ArrayList<Concept>();
    }

    @Override
    protected String getOptionName(Concept concept) {
        return concept.getName();
    }
}
