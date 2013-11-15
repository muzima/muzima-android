/**
 * Copyright 2012 Muzima Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.muzima.adapters.concept;

import android.content.Context;
import android.util.Log;
import android.widget.AutoCompleteTextView;

import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;

import java.util.ArrayList;
import java.util.List;

public class AutoCompleteCohortPrefixAdapter extends AutoCompleteBaseAdapter<Cohort> {

    private static final String TAG = AutoCompleteCohortPrefixAdapter.class.getSimpleName();

    public AutoCompleteCohortPrefixAdapter(Context context, int textViewResourceId, AutoCompleteTextView cohortPrefix) {
        super(context, textViewResourceId, cohortPrefix);
    }

    @Override
    protected List<Cohort> getOptions(CharSequence constraint) {
        CohortController cohortController = getMuzimaApplicationContext().getCohortController();
        try {
            return cohortController.downloadCohortByName(constraint.toString());
        } catch (CohortController.CohortDownloadException e) {
            Log.e(TAG, "Unable to download cohorts!", e);
        }
        return new ArrayList<Cohort>();
    }

    @Override
    protected String getOptionName(Cohort cohort) {
        return cohort.getName();
    }
}
