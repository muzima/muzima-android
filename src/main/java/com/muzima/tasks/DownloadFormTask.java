package com.muzima.tasks;

import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.context.Context;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;

import java.io.File;
import java.util.List;

public class DownloadFormTask extends DownloadTask<String, Void, Integer[]> {
    private static final String TAG = "DownloadFormTask";

    public static final int DOWNLOAD_ERROR = 0;
    public static final int SAVE_ERROR = 1;
    public static final int AUTHENTICATION_ERROR = 2;
    public static final int SUCCESS = 3;
    public static final int CANCELLED = 4;

    private MuzimaApplication applicationContext;

    public DownloadFormTask(MuzimaApplication applicationContext){
        this.applicationContext = applicationContext;
    }

    @Override
    protected Integer[] doInBackground(String... values) {
        Integer[] result = new Integer[2];

        String username = values[0];
        String password = values[1];
        String server = values[2];

        Context context = applicationContext.getMuzimaContext();
        try {
            context.openSession();
            if (!context.isAuthenticated()) {
                context.authenticate(username, password, server);
            }

            if (checkIfTaskIsCancelled(result)) return result;

            FormController formController = applicationContext.getFormController();

            List<Form> forms = formController.downloadAllForms();

            if (checkIfTaskIsCancelled(result)) return result;
            deleteLuceneCache();
            if (!forms.isEmpty()) {
                for (Form form : forms) {
                    formController.saveForm(form);
                }
            }
            result[1] = forms.size();
        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Exception when trying to download forms", e);
            result[0] = DOWNLOAD_ERROR;
            return result;
        } catch (FormController.FormSaveException e) {
            Log.e(TAG, "Exception when trying to save forms", e);
            result[0] = SAVE_ERROR;
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Exception during authentication", e);
            result[0] = AUTHENTICATION_ERROR;
            return result;
        } finally {
            if (context != null)
                context.closeSession();
        }

        result[0] = SUCCESS;
        return result;
    }

    private boolean checkIfTaskIsCancelled(Integer[] result) {
        if(isCancelled()){
            result[0] = CANCELLED;
            return true;
        }
        return false;
    }

    private void deleteLuceneCache() {
        File luceneDirectory = new File(System.getProperty("java.io.tmpdir") + "/lucene");
        int numberOfFiles = luceneDirectory.listFiles().length;
        Log.d(TAG, "Number of files in lucene directory: " + numberOfFiles);
        int fileCounter = 0;
        for (String filename : luceneDirectory.list()) {
            File file = new File(luceneDirectory, filename);
            if(file.delete())   fileCounter++;
        }
        Log.d(TAG, "Number of deleted files: " + fileCounter);
    }
}
