package com.muzima.tasks;

import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.context.Context;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.net.ConnectException;

public abstract class DownloadMuzimaTask extends DownloadTask<String[], Void, Integer[]> {
    private static final String TAG = "DownloadMuzimaTask";

    public static final int DOWNLOAD_ERROR = 0;
    public static final int SAVE_ERROR = 1;
    public static final int AUTHENTICATION_ERROR = 2;
    public static final int DELETE_ERROR = 3;
    public static final int SUCCESS = 4;
    public static final int CANCELLED = 5;
    public static final int CONNECTION_ERROR = 6;
    public static final int PARSING_ERROR = 7;
    public static final int AUTHENTICATION_SUCCESS = 8;


    protected MuzimaApplication applicationContext;

    public DownloadMuzimaTask(MuzimaApplication applicationContext){
        this.applicationContext = applicationContext;
    }

    @Override
    protected Integer[] doInBackground(String[]... values) {
        Integer[] result = new Integer[2];

        int status = authenticate(values[0]);
        if(checkIfTaskIsCancelled(result))  return result;
        if(status != AUTHENTICATION_SUCCESS){
            result[0] = status;
            return result;
        }

        result = performTask(values);
        return result;
    }

    private int authenticate(String[] credentials){
        String username = credentials[0];
        String password = credentials[1];
        String server = credentials[2];
        Context context = applicationContext.getMuzimaContext();
        try {
            context.openSession();
            if (!context.isAuthenticated()) {
                context.authenticate(username, password, server);
            }

        }catch (ConnectException e) {
            Log.e(TAG, "Exception occurred while connecting to server", e);
            return CONNECTION_ERROR;
        } catch (ParseException e) {
            Log.e(TAG, "Exception occurred while authentication phase", e);
            return PARSING_ERROR;
        } catch (IOException e) {
            Log.e(TAG, "Exception occurred while authentication phase", e);
            return AUTHENTICATION_ERROR;
        } finally {
            if (context != null)
                context.closeSession();
        }

        return AUTHENTICATION_SUCCESS;
    }

    protected abstract Integer[] performTask(String[]... values);

    protected boolean checkIfTaskIsCancelled(Integer[] result) {
        if(isCancelled()){
            result[0] = CANCELLED;
            return true;
        }
        return false;
    }
}
