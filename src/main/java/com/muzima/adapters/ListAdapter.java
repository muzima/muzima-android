package com.muzima.adapters;

import android.content.Context;
import android.widget.ArrayAdapter;

public abstract class ListAdapter<T> extends ArrayAdapter<T>{

    public ListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }
    public abstract void reloadData();

    public interface BackgroundListQueryTaskListener{
        public void onQueryTaskStarted();
        public void onQueryTaskFinish();
    }
}
