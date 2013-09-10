package com.muzima.tasks.forms;

import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.FormTemplate;
import com.muzima.controller.FormController;
import com.muzima.tasks.DownloadMuzimaTask;

import java.util.List;

import static com.muzima.controller.FormController.FormFetchException;
import static com.muzima.controller.FormController.FormSaveException;

public class DownloadFormTemplateTask extends DownloadMuzimaTask {
    private static final String TAG = "DownloadFormMetadataTask";

    public DownloadFormTemplateTask(MuzimaApplication applicationContext) {
        super(applicationContext);
    }

    @Override
    protected Integer[] performTask(String[]... values){
        Integer[] result = new Integer[2];

        MuzimaApplication muzimaApplicationContext = getMuzimaApplicationContext();

        if (muzimaApplicationContext == null) {
            result[0] = CANCELLED;
            return result;
        }

        FormController formController = muzimaApplicationContext.getFormController();

        try{
            List<FormTemplate> formTemplates = formController.downloadFormTemplates(values[1]);
            Log.i(TAG, "Form template download successful");

            if (checkIfTaskIsCancelled(result)) return result;

            formController.replaceFormTemplates(formTemplates);
            Log.i(TAG, "Form templates replaced");

            result[0] = SUCCESS;
            result[1] = formTemplates.size();
        } catch (FormSaveException e) {
            Log.e(TAG, "Exception when trying to save forms", e);
            result[0] = SAVE_ERROR;
            return result;
        } catch (FormFetchException e) {
            Log.e(TAG, "Exception when trying to download forms", e);
            result[0] = DOWNLOAD_ERROR;
            return result;
        }
        return result;
    }
}
