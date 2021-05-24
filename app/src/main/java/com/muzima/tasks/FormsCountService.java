package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.controller.FormController;

public class FormsCountService implements Runnable {

    private Context context;
    private FormsCountServiceCallback callback;

    public FormsCountService(Context context, FormsCountServiceCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            long completeForms = ((MuzimaApplication) context.getApplicationContext()).getFormController().countAllCompleteForms();
            long incompleteForms = ((MuzimaApplication) context.getApplicationContext()).getFormController().countAllIncompleteForms();
            callback.onFormsCountLoaded(completeForms, incompleteForms);
        } catch (FormController.FormFetchException ex) {
            ex.printStackTrace();
        }
    }

    public interface FormsCountServiceCallback {
        void onFormsCountLoaded(long completeFormsCount, long incompleteFormsCount);
    }
}
