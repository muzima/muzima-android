package com.muzima.adapters.forms;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.muzima.api.model.Form;
import com.muzima.controller.FormController;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

import java.util.List;

public class DownloadedFormsAdapter extends FormsAdapter {
    private static final String TAG = "DownloadedFormsAdapter";

    public DownloadedFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    public class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask {

        public BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected List<Form> doInBackground(Void... params) {
            List<Form> downloadedForms = null;
            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    downloadedForms = formsAdapter.getFormController().getAllDownloadedFormsByTags(getSelectedTagUuids());
                    Log.i(TAG, "#Forms with templates: " + downloadedForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms " + e);
                }
            }
            return downloadedForms;
        }
    }
}
