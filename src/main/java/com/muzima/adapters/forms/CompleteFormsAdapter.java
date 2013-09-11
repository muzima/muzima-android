package com.muzima.adapters.forms;

import android.content.Context;
import android.util.Log;

import com.muzima.controller.FormController;
import com.muzima.model.CompleteFormWithPatientData;
import com.muzima.model.PatientMetaData;
import com.muzima.model.collections.CompleteFormsWithPatientData;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

import java.util.ArrayList;
import java.util.List;

public class CompleteFormsAdapter extends SectionedFormsAdapter<CompleteFormWithPatientData>{
    private static final String TAG = "CompleteFormsAdapter";

    public CompleteFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    public static class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<CompleteFormWithPatientData> {

        public BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected CompleteFormsWithPatientData doInBackground(Void... voids) {
            CompleteFormsWithPatientData completeForms = null;

            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    completeForms = formsAdapter.getFormController().getAllCompleteForms();
                    Log.i(TAG, "#Complete forms: " + completeForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms " + e);
                }
            }

            return completeForms;
        }

        @Override
        protected void onPostExecute(List<CompleteFormWithPatientData> forms) {
            super.onPostExecute(forms);
            if (adapterWeakReference.get() != null) {
                SectionedFormsAdapter formsAdapter = (SectionedFormsAdapter)adapterWeakReference.get();
                formsAdapter.setPatients(formsAdapter.buildPatientsList(forms));
                formsAdapter.sortFormsByPatientName();
            }
        }
    }
}
