package com.muzima.adapters.forms;

import android.content.Context;
import android.util.Log;

import com.muzima.controller.FormController;
import com.muzima.model.IncompleteForm;
import com.muzima.model.IncompleteFormWithPatientData;
import com.muzima.model.collections.IncompleteFormsWithPatientData;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

public class IncompleteFormsAdapter extends FormsAdapter<IncompleteForm> {
    private static final String TAG = "IncompleteFormsAdapter";

    public IncompleteFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

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
                    Log.w(TAG, "Exception occurred while fetching local forms " + e);
                }
            }
            return incompleteForms;
        }
    }
}
