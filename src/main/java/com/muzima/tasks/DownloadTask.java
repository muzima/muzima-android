package com.muzima.tasks;

import android.os.AsyncTask;

import com.muzima.listeners.DownloadListener;

public abstract class DownloadTask<Pa, Pr, Re> extends AsyncTask<Pa, Pr, Re> {

    private DownloadListener mStateListener;

    @Override
    protected void onPostExecute(Re result) {
        synchronized (this) {
            if (mStateListener != null) {
                mStateListener.downloadTaskComplete(result);
            }
        }
    }

    public void setDownloadListener(DownloadListener dl) {
        synchronized (this) {
            mStateListener = dl;
        }
    }
}