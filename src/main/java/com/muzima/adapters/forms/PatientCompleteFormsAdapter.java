package com.muzima.adapters.forms;

import android.content.Context;
import android.util.Log;

import com.muzima.controller.FormController;
import com.muzima.model.CompletePatientForm;
import com.muzima.model.collections.CompletePatientForms;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

public class PatientCompleteFormsAdapter extends FormsAdapter {
    private static final String TAG = "CompleteFormsAdapter";
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

    public static class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<CompletePatientForm> {

        public BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected CompletePatientForms doInBackground(Void... voids) {
            CompletePatientForms completePatientForms = null;

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
