package com.muzima.tasks;

import android.os.AsyncTask;

import com.muzima.api.model.Form;

import java.util.List;

import static com.muzima.adapters.ListAdapter.BackgroundListQueryTaskListener;

public abstract class QueryTask extends AsyncTask<Void, Void, List<Form>> {

    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    public QueryTask(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }


    @Override
    protected void onPreExecute() {
        if(backgroundListQueryTaskListener != null){
            backgroundListQueryTaskListener.onQueryTaskStarted();
        }
    }


    @Override
    protected void onPostExecute(List<Form> forms) {
        if(backgroundListQueryTaskListener != null){
            backgroundListQueryTaskListener.onQueryTaskFinish();
        }
    }
}
