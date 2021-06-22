package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;

import java.util.List;

public class LoadAllFormsTask implements Runnable {
    private Context context;
    private FormsLoadedCallback callback;

    public LoadAllFormsTask(Context context, FormsLoadedCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            callback.onFormsLoaded(((MuzimaApplication) context.getApplicationContext()).getFormController()
                    .getAllAvailableForms());
        } catch (FormController.FormFetchException ex) {
            ex.printStackTrace();
        }
    }

    public interface FormsLoadedCallback {
        void onFormsLoaded(List<Form> formList);
    }
}
