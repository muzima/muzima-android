package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.controller.FormController;
import com.muzima.model.CompleteFormWithPatientData;
import com.muzima.model.IncompleteFormWithPatientData;

import java.util.List;

public class LoadFormsWithDataTask implements Runnable {
    private Context context;
    private String filter;
    private boolean incompleteFormsLoad;
    private LoadFormsFinishedCallback callback;

    public LoadFormsWithDataTask(Context context, String filter, boolean incompleteFormsLoad, LoadFormsFinishedCallback callback) {
        this.context = context;
        this.filter = filter;
        this.incompleteFormsLoad = incompleteFormsLoad;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            if (incompleteFormsLoad){
                callback.onIncompleteFormsLoaded(((MuzimaApplication) context.getApplicationContext()).getFormController()
                        .getAllIncompleteFormsWithPatientData());
            }else {
                callback.onCompleteFormsLoaded(((MuzimaApplication) context.getApplicationContext()).getFormController()
                        .getAllCompleteFormsWithPatientData(context));
            }

        } catch (FormController.FormFetchException ex) {
            ex.printStackTrace();
        }
    }

    public interface LoadFormsFinishedCallback {
        void onIncompleteFormsLoaded(List<IncompleteFormWithPatientData> formList);
        void onCompleteFormsLoaded(List<CompleteFormWithPatientData> formList);
    }
}
