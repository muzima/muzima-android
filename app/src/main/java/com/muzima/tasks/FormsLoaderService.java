package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.controller.FormController;
import com.muzima.model.DownloadedForm;

import java.util.List;

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
            List<DownloadedForm> formsList = ((MuzimaApplication) context.getApplicationContext()).getFormController()
                    .getAllDownloadedForms();
            callback.onFormsLoaded(formsList);
        }catch (FormController.FormFetchException ex){
            ex.printStackTrace();
        }
    }

    public interface FormsLoadedCallback {
        void onFormsLoaded(List<DownloadedForm> formList);
    }
}
