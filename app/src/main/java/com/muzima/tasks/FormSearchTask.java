package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;

import java.util.List;

public class FormSearchTask implements Runnable {

    private Context context;
    private String searchTerm;
    private FormSearchCallback formSearchCallback;

    public FormSearchTask(Context context, String searchTerm, FormSearchCallback formSearchCallback) {
        this.context = context;
        this.searchTerm = searchTerm;
        this.formSearchCallback = formSearchCallback;
    }

    @Override
    public void run() {
        try {
            List<Form> availableForms = ((MuzimaApplication) context.getApplicationContext()).getFormController()
                    .getAllAvailableForms();
//            formSearchCallback.onFormSearchCompleted(availableForms);
        } catch (FormController.FormFetchException ex) {
            ex.printStackTrace();
        }
    }

    public interface FormSearchCallback {
        void onFormSearchCompleted(List<AvailableForm> formList);
    }
}
