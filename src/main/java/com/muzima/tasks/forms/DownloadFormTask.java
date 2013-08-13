package com.muzima.tasks.forms;

import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.context.Context;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;
import com.muzima.tasks.DownloadTask;

import org.apache.lucene.queryParser.ParseException;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.List;

public abstract class DownloadFormTask extends DownloadTask<String[], Void, Integer[]> {
    private static final String TAG = "DownloadFormTask";

    public static final int DOWNLOAD_ERROR = 0;
    public static final int SAVE_ERROR = 1;
    public static final int AUTHENTICATION_ERROR = 2;
    public static final int DELETE_ERROR = 3;
    public static final int SUCCESS = 4;
    public static final int CANCELLED = 5;
    public static final int CONNECTION_ERROR = 6;
    public static final int PARSING_ERROR = 7;

    protected MuzimaApplication applicationContext;

    public DownloadFormTask(MuzimaApplication applicationContext){
        this.applicationContext = applicationContext;
    }

    @Override
    protected Integer[] doInBackground(String[]... values) {
        Integer[] result = new Integer[2];

        String[] credentials = values[0];
        String username = credentials[0];
        String password = credentials[1];
        String server = credentials[2];

        Context context = applicationContext.getMuzimaContext();
        try {
            context.openSession();
            if (!context.isAuthenticated()) {
                context.authenticate(username, password, server);
            }

            if (checkIfTaskIsCancelled(result)) return result;

            String[] dataToDownload = null;
            if(values.length > 1){
                dataToDownload = values[1];
            }
            result = performTask(dataToDownload);


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
        }catch (ConnectException e) {
            Log.e(TAG, "Exception occurred while connecting to server", e);
            result[0] = CONNECTION_ERROR;
            return result;
        } catch (ParseException e) {
            Log.e(TAG, "Exception occurred while authentication phase", e);
            result[0] = AUTHENTICATION_ERROR;
            return result;
        } catch (IOException e) {
            Log.e(TAG, "Exception occurred while authentication phase", e);
            result[0] = AUTHENTICATION_ERROR;
            return result;
        } finally {
            if (context != null)
                context.closeSession();
        }

        return result;
    }

    protected abstract Integer[] performTask(String[] values) throws FormController.FormFetchException, FormController.FormDeleteException, FormController.FormSaveException;

    protected boolean checkIfTaskIsCancelled(Integer[] result) {
        if(isCancelled()){
            result[0] = CANCELLED;
            return true;
        }
        return false;
    }
}
