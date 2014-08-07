/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

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

package com.muzima.adapters.forms;

import android.content.Context;
import android.util.Log;
import com.muzima.controller.FormController;
import com.muzima.model.IncompleteFormWithPatientData;
import com.muzima.model.collections.IncompleteFormsWithPatientData;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

import java.util.List;

/**
 * Responsible to display all the incomplete forms.
 */
public class IncompleteFormsAdapter extends SectionedFormsAdapter<IncompleteFormWithPatientData> {
    private static final String TAG = "IncompleteFormsAdapter";

    public IncompleteFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    /**
     * Responsible to fetch all the incomplete forms from the DB.
     */
    public static class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<IncompleteFormWithPatientData> {

        public BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected IncompleteFormsWithPatientData doInBackground(Void... voids) {
            IncompleteFormsWithPatientData incompleteForms = null;

            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    incompleteForms = formsAdapter.getFormController().getAllIncompleteForms();
                    Log.i(TAG, "#Incomplete forms: " + incompleteForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms ", e);
                }
            }
            return incompleteForms;
        }

        @Override
        protected void onPostExecute(List<IncompleteFormWithPatientData> forms) {
            if (adapterWeakReference.get() != null) {
                SectionedFormsAdapter formsAdapter = (SectionedFormsAdapter)adapterWeakReference.get();
                formsAdapter.setPatients(formsAdapter.buildPatientsList(forms));
                formsAdapter.sortFormsByPatientName(forms);
                notifyListener();
            }
        }
    }
}
