/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters;

import android.content.Context;
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
