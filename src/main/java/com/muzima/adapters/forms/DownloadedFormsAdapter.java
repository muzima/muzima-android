package com.muzima.adapters.forms;

import android.content.Context;
import android.util.Log;

import com.muzima.controller.FormController;
import com.muzima.model.DownloadedForm;
import com.muzima.model.collections.DownloadedForms;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

import java.util.List;

public class DownloadedFormsAdapter extends FormsAdapter<DownloadedForm> {
    private static final String TAG = "DownloadedFormsAdapter";

    public DownloadedFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    public class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<DownloadedForm> {

        public BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected List<DownloadedForm> doInBackground(Void... params) {
            DownloadedForms downloadedForms = null;
            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    downloadedForms = formsAdapter.getFormController().getAllDownloadedFormsByTags();
                    Log.i(TAG, "#Forms with templates: " + downloadedForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms " + e);
                }
            }
            return downloadedForms;
        }
    }
}
