package com.muzima.adapters.forms;

import android.content.Context;
import android.util.Log;

import com.muzima.api.model.Form;
import com.muzima.controller.FormController;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

import java.util.List;

public abstract class CompleteFormsAdapter extends FormsAdapter {
    private static final String TAG = "CompleteFormsAdapter";

    public CompleteFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    protected abstract List<Form> fetchForms() throws FormController.FormFetchException;

    public static class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask {

        public BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected List<Form> doInBackground(Void... voids) {
            List<Form> downloadedForms = null;

            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    downloadedForms = ((CompleteFormsAdapter)formsAdapter).fetchForms();

                    Log.i(TAG, "#Complete forms: " + downloadedForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms " + e);
                }
            }

            return downloadedForms;
        }

    }
}
