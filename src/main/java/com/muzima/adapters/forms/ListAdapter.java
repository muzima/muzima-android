package com.muzima.adapters.forms;

import android.content.Context;
import android.widget.ArrayAdapter;

public abstract class ListAdapter<T> extends ArrayAdapter<T>{

    public ListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }
    public abstract void reloadData();
}
