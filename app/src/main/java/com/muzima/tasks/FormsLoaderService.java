/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.controller.FormController;
import com.muzima.model.collections.AvailableForms;

public class FormsLoaderService implements Runnable {

    private Context context;
    private FormsLoadedCallback callback;

    public FormsLoaderService(Context context, FormsLoadedCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            AvailableForms formsList = ((MuzimaApplication) context.getApplicationContext()).getFormController()
                    .getRecommendedForms();
            callback.onFormsLoaded(formsList);
        }catch (FormController.FormFetchException ex){
            ex.printStackTrace();
        }
    }

    public interface FormsLoadedCallback {
        void onFormsLoaded(AvailableForms availableForms);
    }
}
