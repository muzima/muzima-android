/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.adapters.forms;

import android.content.Context;
import android.util.Log;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.model.IncompleteFormWithPatientData;
import com.muzima.model.collections.IncompleteFormsWithPatientData;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

import java.util.List;

/**
 * Responsible to display all the incomplete forms.
 */
public class IncompleteFormsWithDataAdapter extends FormsWithDataAdapter<IncompleteFormWithPatientData> {

    private static String filterPatientUuid;
    public IncompleteFormsWithDataAdapter(Context context, int textViewResourceId, String filterPatientUuid, FormController formController, ObservationController observationController) {
        super(context, textViewResourceId, filterPatientUuid, formController, observationController);
        this.filterPatientUuid = filterPatientUuid;
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    /**
     * Responsible to fetch all the incomplete forms from the DB.
     */
    protected static class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<IncompleteFormWithPatientData> {

        BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected IncompleteFormsWithPatientData doInBackground(Void... voids) {
            IncompleteFormsWithPatientData incompleteForms = null;

            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    incompleteForms = formsAdapter.getFormController().getAllIncompleteFormsWithPatientData(filterPatientUuid);
                    Log.i(getClass().getSimpleName(), "#Incomplete forms: " + incompleteForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(getClass().getSimpleName(), "Exception occurred while fetching local forms ", e);
                }
            }
            return incompleteForms;
        }

        @Override
        protected void onPostExecute(List<IncompleteFormWithPatientData> forms) {
            if (adapterWeakReference.get() != null) {
                FormsWithDataAdapter formsAdapter = (FormsWithDataAdapter) adapterWeakReference.get();
                if (forms != null && !forms.isEmpty()) {
                    formsAdapter.sortFormsByPatientName(forms);
                    notifyListener();
                } else {
                    formsAdapter.clear();
                    notifyListener();
                }
            }
        }

        @Override
        protected void onBackgroundError(Exception e) {

        }
    }
}
