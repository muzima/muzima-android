package com.muzima.tasks.forms;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;

import java.util.List;

import static com.muzima.controller.FormController.FormDeleteException;
import static com.muzima.controller.FormController.FormFetchException;
import static com.muzima.controller.FormController.FormSaveException;

public class DownloadFormMetadataTask extends DownloadFormTask {
    private static final String TAG = "DownloadFormMetadataTask";

    public DownloadFormMetadataTask(MuzimaApplication applicationContext) {
        super(applicationContext);
    }

    @Override
    protected Integer[] performTask(String[] values) throws FormDeleteException, FormSaveException, FormFetchException {
        Integer[] result = new Integer[2];
        FormController formController = applicationContext.getFormController();

        List<Form> forms = formController.downloadAllForms();

        if (checkIfTaskIsCancelled(result)) return result;

        formController.deleteAllForms();
        formController.saveAllForms(forms);
        result[0] = SUCCESS;
        result[1] = forms.size();

        return result;
    }
}
