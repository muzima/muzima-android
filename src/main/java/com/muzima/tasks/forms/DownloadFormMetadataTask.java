package com.muzima.tasks.forms;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.utils.Constants;
import com.muzima.view.forms.NewFormsListFragment;

import java.util.Date;
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
            Log.i(TAG, "Form download successful, " + isCancelled());
            if (checkIfTaskIsCancelled(result)) return result;

            formController.deleteAllForms();
            Log.i(TAG, "Old forms are deleted");
            formController.saveAllForms(forms);
            Log.i(TAG, "New forms are saved");

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

    @Override
    protected void onPostExecute(Integer[] result) {
        SharedPreferences pref = applicationContext.getSharedPreferences(Constants.SYNC_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        Date date = new Date();
        editor.putLong(NewFormsListFragment.LAST_SYNCED_TIME, date.getTime());
        editor.commit();
        super.onPostExecute(result);
    }
}
