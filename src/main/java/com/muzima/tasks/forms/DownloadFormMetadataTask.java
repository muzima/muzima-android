package com.muzima.tasks.forms;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.utils.Constants;
import com.muzima.view.forms.AllAvailableFormsListFragment;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DownloadFormMetadataTask extends DownloadMuzimaTask {
    private static final String TAG = "DownloadFormMetadataTask";

    public DownloadFormMetadataTask(MuzimaApplication applicationContext) {
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
        List<String> tags = getTags(muzimaApplicationContext);

        try {
            List<Form> forms;
            if (tags.isEmpty()) {
                forms = formController.downloadAllForms();
            } else {
                forms = formController.downloadFormsByTags(tags);
            }
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

    private List<String> getTags(MuzimaApplication muzimaApplicationContext) {
        SharedPreferences sharedPreferences = muzimaApplicationContext.getSharedPreferences(Constants.FORM_TAG_PREF, Context.MODE_PRIVATE);
        Set<String> tags = sharedPreferences.getStringSet(Constants.FORM_TAG_PREF_KEY, new HashSet<String>());
        return new ArrayList<String>(tags);
    }

    @Override
    protected void onPostExecute(Integer[] result) {
        MuzimaApplication muzimaApplicationContext = getMuzimaApplicationContext();

        if (muzimaApplicationContext == null) {
            return;
        }

        SharedPreferences pref = muzimaApplicationContext.getSharedPreferences(Constants.SYNC_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        Date date = new Date();
        editor.putLong(AllAvailableFormsListFragment.FORMS_METADATA_LAST_SYNCED_TIME, date.getTime());
        editor.commit();
        super.onPostExecute(result);
    }
}
