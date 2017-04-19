/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.adapters.forms;

import android.content.Context;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.collections.AvailableForms;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

/**
 * Responsible to list down all the registration forms. Will be in use only if there are more than 1 form.
 */
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
