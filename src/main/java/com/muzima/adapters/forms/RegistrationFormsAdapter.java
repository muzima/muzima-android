package com.muzima.adapters.forms;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.collections.AvailableForms;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

public class RegistrationFormsAdapter extends FormsAdapter<AvailableForm>{
    private static final String TAG = "RegistrationFormsAdapter";


    public RegistrationFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    public class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<AvailableForm> {

        public BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected AvailableForms doInBackground(Void... voids) {
            AvailableForms registrationForm = null;
            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    registrationForm = formsAdapter.getFormController().getDownloadedRegistrationForms();
                    Log.i(TAG, "#Forms: " + registrationForm.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms " + e);
                }
            }
            return registrationForm;
        }
    }

}
