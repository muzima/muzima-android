package com.muzima.tasks.forms;

import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;
import com.muzima.tasks.DownloadMuzimaTask;

import java.util.List;

public class DownloadFormMetadataTask extends DownloadMuzimaTask {
    private static final String TAG = "DownloadFormMetadataTask";

    public DownloadFormMetadataTask(MuzimaApplication applicationContext) {
        super(applicationContext);
    }

    @Override
    protected Integer[] performTask(String[]... values){
        Integer[] result = new Integer[2];
        FormController formController = applicationContext.getFormController();

        try {
            List<Form> forms = formController.downloadAllForms();

            if (checkIfTaskIsCancelled(result)) return result;

            formController.deleteAllForms();
            formController.saveAllForms(forms);
            result[0] = SUCCESS;
            result[1] = forms.size();

        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Exception when trying to download forms", e);
            result[0] = DOWNLOAD_ERROR;
            return result;
        } catch (FormController.FormSaveException e) {
            Log.e(TAG, "Exception when trying to save forms", e);
            result[0] = SAVE_ERROR;
            return result;
        } catch (FormController.FormDeleteException e) {
            Log.e(TAG, "Exception occurred while deleting existing forms", e);
            result[0] = DELETE_ERROR;
            return result;
        }
        return result;
    }
}
