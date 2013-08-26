package com.muzima.tasks;

import android.os.AsyncTask;

import com.muzima.listeners.DownloadListener;

import java.util.ArrayList;
import java.util.List;

public abstract class DownloadTask<Pa, Pr, Re> extends AsyncTask<Pa, Pr, Re> {

    private List<DownloadListener> mStateListener = new ArrayList<DownloadListener>();

    @Override
    protected void onPreExecute() {
        for (DownloadListener downloadListener : mStateListener) {
            downloadListener.downloadTaskStart();
        }
    }

    @Override
    protected void onPostExecute(Re result) {
        for (DownloadListener downloadListener : mStateListener) {
            downloadListener.downloadTaskComplete(result);
        }
    }

    public void addDownloadListener(DownloadListener dl) {
        mStateListener.add(dl);
    }
}