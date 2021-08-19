package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LoadAllFormsTask implements Runnable {
    private Context context;
    private String searchKey;
    private FormsLoadedCallback callback;

    public LoadAllFormsTask(Context context, String searchKey, FormsLoadedCallback callback) {
        this.context = context;
        this.searchKey = searchKey;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            if (searchKey == null || searchKey.isEmpty())
                callback.onFormsLoaded(((MuzimaApplication) context.getApplicationContext()).getFormController()
                        .getAllAvailableForms());
            else {
                List<Form> searchResult = new ArrayList<>();
                for (Form allAvailableForm : ((MuzimaApplication) context.getApplicationContext()).getFormController()
                        .getAllAvailableForms()) {
                    if (allAvailableForm.getName().toLowerCase().contains(searchKey.toLowerCase()) ||
                            allAvailableForm.getDescription().toLowerCase().contains(searchKey.toLowerCase())) {
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
