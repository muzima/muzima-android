/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.adapters.forms;

import android.content.Context;
import android.util.Log;
import com.muzima.R;
import com.muzima.controller.FormController;
import com.muzima.model.IncompleteForm;
import com.muzima.model.collections.IncompleteForms;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

/**
 * Responsible to list down all the incomplete forms for a specific patient.
 */
public class PatientIncompleteFormsAdapter extends FormsWithDataAdapter<IncompleteForm> {
    private final String patientId;

    public PatientIncompleteFormsAdapter(Context context, int textViewResourceId, FormController formController, String patientId) {
        super(context, textViewResourceId, formController);
        this.patientId = patientId;
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    private String getPatientId() {
        return patientId;
    }

    @Override
    protected int getFormItemLayout() {
        return R.layout.item_forms_list_selectable;
    }


    /**
     * Responsible to fetch all the incomplete forms for a specific patient.
     */
    static class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<IncompleteForm> {

        BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected IncompleteForms doInBackground(Void... voids) {
            IncompleteForms incompleteForms = null;

            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    incompleteForms = formsAdapter.getFormController()
                            .getAllIncompleteFormsForPatientUuid(((PatientIncompleteFormsAdapter) formsAdapter).getPatientId());

                    Log.i(getClass().getSimpleName(), "#Incomplete forms: " + incompleteForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(getClass().getSimpleName(), "Exception occurred while fetching local forms ", e);
                }
            }

            return incompleteForms;
        }
    }
}
