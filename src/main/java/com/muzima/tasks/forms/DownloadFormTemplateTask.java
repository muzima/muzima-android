package com.muzima.tasks.forms;

import com.muzima.MuzimaApplication;

import static com.muzima.controller.FormController.FormDeleteException;
import static com.muzima.controller.FormController.FormFetchException;
import static com.muzima.controller.FormController.FormSaveException;

public class DownloadFormTemplateTask extends DownloadFormTask {
    private static final String TAG = "DownloadFormMetadataTask";

    public DownloadFormTemplateTask(MuzimaApplication applicationContext) {
        super(applicationContext);
    }

    @Override
    protected Integer[] performTask() throws FormDeleteException, FormSaveException, FormFetchException {
        Integer[] result = new Integer[2];
        return result;
    }
}
