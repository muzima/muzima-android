package com.muzima.tasks;

import android.content.Context;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.controller.FormController;

public class FormsCountService implements Runnable {
    private static final String TAG = "FormsCountService";
    private String patientUuid;
    private Context context;
    private FormsCountServiceCallback callback;

    public FormsCountService(String patientUuid, Context context, FormsCountServiceCallback callback) {
        this.patientUuid = patientUuid;
        this.context = context;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            Log.e(TAG, "run:  FormsCountService patientId " + patientUuid);
            long completeForms = ((MuzimaApplication) context.getApplicationContext()).getFormController().getCompleteFormsCountForPatient(patientUuid);
            long incompleteForms = ((MuzimaApplication) context.getApplicationContext()).getFormController().getIncompleteFormsCountForPatient(patientUuid);
            Log.e(TAG, "run: completeForms " + completeForms + " incompleteForms " + incompleteForms);
            callback.onFormsCountLoaded(completeForms, incompleteForms);
        } catch (FormController.FormFetchException ex) {
            ex.printStackTrace();
        }
    }

    public interface FormsCountServiceCallback {
        void onFormsCountLoaded(long completeFormsCount, long incompleteFormsCount);
    }
}
