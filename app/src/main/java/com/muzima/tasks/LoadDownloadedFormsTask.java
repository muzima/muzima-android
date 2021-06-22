package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;

import java.util.ArrayList;
import java.util.List;

public class LoadDownloadedFormsTask implements Runnable {
    private Context context;
    private String searchKey;
    private LoadAllFormsTask.FormsLoadedCallback callback;

    public LoadDownloadedFormsTask(Context context, String searchKey, LoadAllFormsTask.FormsLoadedCallback callback) {
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
                    if (((MuzimaApplication) context.getApplicationContext()).getFormController().isFormDownloaded(form))
                        forms.add(form);
                }
                callback.onFormsLoaded(forms);
            }else {
                List<Form> searchResult = new ArrayList<>();
                for (Form allAvailableForm : ((MuzimaApplication) context.getApplicationContext()).getFormController()
                        .getAllAvailableForms()) {
                    if (allAvailableForm.getName().toLowerCase().startsWith(searchKey.toLowerCase()) || allAvailableForm.getDescription().toLowerCase().startsWith(searchKey.toLowerCase())) {
                        if (((MuzimaApplication) context.getApplicationContext()).getFormController().isFormDownloaded(allAvailableForm))
                            searchResult.add(allAvailableForm);
                    }
                }
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