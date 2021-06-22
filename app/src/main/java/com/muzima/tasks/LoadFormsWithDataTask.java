package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.controller.FormController;
import com.muzima.model.CompleteFormWithPatientData;

import java.util.List;

public class LoadFormsWithDataTask implements Runnable {
    private Context context;
    private String filter;
    private LoadFormsFinishedCallback callback;

    public LoadFormsWithDataTask(Context context, String filter, LoadFormsFinishedCallback callback) {
        this.context = context;
        this.filter = filter;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            callback.onFormsLoaded(((MuzimaApplication) context.getApplicationContext()).getFormController()
                    .getAllCompleteFormsWithPatientData(context));
        } catch (FormController.FormFetchException ex) {
            ex.printStackTrace();
        }
    }

    public interface LoadFormsFinishedCallback {
        void onFormsLoaded(List<CompleteFormWithPatientData> formList);
    }
}
