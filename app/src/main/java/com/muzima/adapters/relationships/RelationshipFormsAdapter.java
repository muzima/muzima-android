/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.relationships;

import android.content.Context;

import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.collections.AvailableForms;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

public class RelationshipFormsAdapter extends FormsAdapter<AvailableForm> {
    private final AvailableForms availableForms;


    public RelationshipFormsAdapter(Context context, int textViewResourceId, FormController formController, AvailableForms availableForms) {
        super(context, textViewResourceId, formController);
        this.availableForms = availableForms;
    }

    @Override
    public void reloadData() {
        new RelationshipFormsAdapter.BackgroundQueryTask(this).execute();
    }

    class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<AvailableForm> {

        BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected AvailableForms doInBackground(Void... voids) {
            return availableForms;
        }
    }
}
