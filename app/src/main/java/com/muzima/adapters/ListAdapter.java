/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters;

import android.content.Context;
import android.widget.ArrayAdapter;

public abstract class ListAdapter<T> extends ArrayAdapter<T>{

    protected ListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public abstract void reloadData();

    public interface BackgroundListQueryTaskListener{
        void onQueryTaskStarted();
        void onQueryTaskFinish();
        void onQueryTaskCancelled();
        void onQueryTaskCancelled(Object errorDefinition);
    }
}