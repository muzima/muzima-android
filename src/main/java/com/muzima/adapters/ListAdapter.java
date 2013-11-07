package com.muzima.adapters;

import android.content.Context;
import android.os.Build;
import android.widget.ArrayAdapter;

import java.util.Collection;

public abstract class ListAdapter<T> extends ArrayAdapter<T>{

    public ListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public abstract void reloadData();

    public interface BackgroundListQueryTaskListener{
        public void onQueryTaskStarted();
        public void onQueryTaskFinish();
    }

    // addAll is not supported in API 8, so overwrite it
    @Override
    public void addAll(Collection<? extends T> collection) {
        for (T t : collection) {
            add(t);
        }
    }
}
