/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.adapters.concept;

import android.content.Context;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible to display and select auto-complete menu of CohortPrefix.
 */
public class AutoCompleteCohortPrefixAdapter extends AutoCompleteBaseAdapter<Cohort> {


    public AutoCompleteCohortPrefixAdapter(Context context, int textViewResourceId, AutoCompleteTextView cohortPrefix) {
        super(context, textViewResourceId, cohortPrefix);
    }

    @Override
    protected List<Cohort> getOptions(CharSequence constraint) {
        CohortController cohortController = getMuzimaApplicationContext().getCohortController();
        try {
            return cohortController.downloadCohortByName(constraint.toString());
        } catch (CohortController.CohortDownloadException e) {
            Log.e(getClass().getSimpleName(), "Unable to download cohorts!", e);
        }
        return new ArrayList<>();
    }

    @Override
    protected String getOptionName(Cohort cohort) {
        return cohort.getName();
    }
}
