package com.muzima.adapters.forms;

import android.content.Context;
import android.util.Log;

import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.collections.AvailableForms;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

import java.util.List;

public class RecommendedFormsAdapter extends FormsAdapter<AvailableForm> {
    private static final String TAG = "RecommendedFormsAdapter";

    public RecommendedFormsAdapter(Context context, int textViewResourceId, FormController formController) {
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
        protected List<AvailableForm> doInBackground(Void... params) {
            AvailableForms recommendedForms = null;
            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    recommendedForms = formsAdapter.getFormController().getRecommendedForms();
                    Log.i(TAG, "#Forms with templates: " + recommendedForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms " + e);
                }
            }
            return recommendedForms;
        }
    }
}
