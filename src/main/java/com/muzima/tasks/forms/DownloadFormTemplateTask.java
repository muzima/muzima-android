package com.muzima.tasks.forms;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.FormTemplate;
import com.muzima.controller.FormController;

import java.util.List;

import static com.muzima.controller.FormController.FormDeleteException;
import static com.muzima.controller.FormController.FormFetchException;
import static com.muzima.controller.FormController.FormSaveException;

public class DownloadFormTemplateTask extends DownloadFormTask {
    private static final String TAG = "DownloadFormMetadataTask";

    public DownloadFormTemplateTask(MuzimaApplication applicationContext) {
        super(applicationContext);
    }

    @Override
    protected Integer[] performTask(String[] values) throws FormDeleteException, FormSaveException, FormFetchException {
        Integer[] result = new Integer[2];
        FormController formController = applicationContext.getFormController();

        List<FormTemplate> formTemplates = formController.downloadFormTemplates(values);

        if (checkIfTaskIsCancelled(result)) return result;

        formController.replaceFormTemplates(formTemplates);

        result[0] = SUCCESS;
        result[1] = formTemplates.size();
        return result;
    }
}
