package com.muzima.adapters.forms;

import android.content.Context;
import android.util.Log;

import com.muzima.controller.FormController;
import com.muzima.model.CompleteForm;
import com.muzima.model.collections.CompleteForms;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

public class PatientCompleteFormsAdapter extends FormsAdapter {
    private static final String TAG = "PatientCompleteFormsAdapter";
    private String patientId;

    public PatientCompleteFormsAdapter(Context context, int textViewResourceId, FormController formController, String patientId) {
        super(context, textViewResourceId, formController);
        this.patientId = patientId;
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    public String getPatientId() {
        return patientId;
    }

    public static class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<CompleteForm> {

        public BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected CompleteForms doInBackground(Void... voids) {
            CompleteForms completePatientForms = null;

            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    completePatientForms = formsAdapter.getFormController()
                            .getAllCompleteFormsForPatientUuid(((PatientCompleteFormsAdapter) formsAdapter).getPatientId());

                    Log.i(TAG, "#Complete forms: " + completePatientForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms " + e);
                }
            }

            return completePatientForms;
        }
    }
}
