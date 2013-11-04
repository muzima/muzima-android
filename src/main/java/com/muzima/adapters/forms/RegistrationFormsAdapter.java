package com.muzima.adapters.forms;

import android.content.Context;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.collections.AvailableForms;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

public class RegistrationFormsAdapter extends FormsAdapter<AvailableForm> {
    private static final String TAG = "RegistrationFormsAdapter";
    private AvailableForms availableForms;


    public RegistrationFormsAdapter(Context context, int textViewResourceId, FormController formController, AvailableForms availableForms) {
        super(context, textViewResourceId, formController);
        this.availableForms = availableForms;
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
            return availableForms;
        }
    }

}
