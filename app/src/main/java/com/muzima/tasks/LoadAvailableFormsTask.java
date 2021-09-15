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
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LoadAvailableFormsTask implements Runnable {
    private Context context;
    private String searchKey;
    private LoadAllFormsTask.FormsLoadedCallback callback;

    public LoadAvailableFormsTask(Context context, String searchKey, LoadAllFormsTask.FormsLoadedCallback callback) {
        this.context = context;
        this.searchKey = searchKey;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            if (searchKey == null || searchKey.isEmpty()) {
                List<Form> forms = new ArrayList<>();
                for (Form form : ((MuzimaApplication) context.getApplicationContext()).getFormController()
                        .getAllAvailableForms()) {
                    if (!((MuzimaApplication) context.getApplicationContext()).getFormController().isFormDownloaded(form))
                        forms.add(form);
                }
                callback.onFormsLoaded(forms);
            } else {
                List<Form> searchResult = new ArrayList<>();
                for (Form allAvailableForm : ((MuzimaApplication) context.getApplicationContext()).getFormController()
                        .getAllAvailableForms()) {
                    if (allAvailableForm.getName().toLowerCase().startsWith(searchKey.toLowerCase()) || allAvailableForm.getDescription().toLowerCase().startsWith(searchKey.toLowerCase())) {
                        if (!((MuzimaApplication) context.getApplicationContext()).getFormController().isFormDownloaded(allAvailableForm))
                            searchResult.add(allAvailableForm);
                    }
                }
                Collections.sort(searchResult, new Comparator<Form>() {
                    @Override
                    public int compare(Form o1, Form o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                callback.onFormsLoaded(searchResult);
            }
        } catch (FormController.FormFetchException ex) {
            ex.printStackTrace();
        }
    }

    public interface FormsLoadedCallback {
        void onFormsLoaded(List<Form> formList);
    }
}
