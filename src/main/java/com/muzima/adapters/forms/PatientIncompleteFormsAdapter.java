package com.muzima.adapters.forms;

import android.content.Context;
import android.util.Log;

import com.muzima.controller.FormController;
import com.muzima.model.collections.IncompleteForms;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

public class PatientIncompleteFormsAdapter extends FormsAdapter {
    private static final String TAG = "PatientIncompleteFormsAdapter";
    private String patientId;

    public PatientIncompleteFormsAdapter(Context context, int textViewResourceId, FormController formController, String patientId) {
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

    public static class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<IncompleteForm> {

        public BackgroundQueryTask(FormsAdapter formsAdapter) {
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

                    Log.i(TAG, "#Complete forms: " + incompleteForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms " + e);
                }
            }

            return incompleteForms;
        }
    }
}
