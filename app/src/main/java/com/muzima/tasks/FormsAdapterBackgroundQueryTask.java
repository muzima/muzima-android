/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.tasks;

import android.os.AsyncTask;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.model.BaseForm;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.muzima.adapters.ListAdapter.BackgroundListQueryTaskListener;

public abstract class FormsAdapterBackgroundQueryTask<T extends BaseForm> extends AsyncTask<Void, Void, List<T>> {

    protected final WeakReference<FormsAdapter> adapterWeakReference;

    protected FormsAdapterBackgroundQueryTask(FormsAdapter adapter) {
        adapterWeakReference = new WeakReference<>(adapter);
    }


    @Override
    protected void onPreExecute() {
        if (adapterWeakReference.get() != null) {
            FormsAdapter formsAdapter = adapterWeakReference.get();
            BackgroundListQueryTaskListener backgroundListQueryTaskListener = formsAdapter.getBackgroundListQueryTaskListener();
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }
    }

    @Override
    protected void onPostExecute(List<T> forms) {
        if (forms==null) {
            Toast.makeText(adapterWeakReference.get().getContext(), adapterWeakReference.get().getContext().getString(R.string.error_form_load), Toast.LENGTH_SHORT).show();
        }

        changeDataSet(forms);
        notifyListener();
    }

    protected void notifyListener() {
        if (adapterWeakReference.get() != null) {
            FormsAdapter formsAdapter = adapterWeakReference.get();
            BackgroundListQueryTaskListener backgroundListQueryTaskListener = formsAdapter.getBackgroundListQueryTaskListener();
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskFinish();
            }
        }
    }

    private void changeDataSet(List<T> forms) {
        if (adapterWeakReference.get() != null) {
            FormsAdapter formsAdapter = adapterWeakReference.get();
            if (forms == null) {
                Toast.makeText(formsAdapter.getContext(), formsAdapter.getContext().getString(R.string.error_form_fetch), Toast.LENGTH_SHORT).show();
                return;
            }
            formsAdapter.clear();
            formsAdapter.addAll(forms);
            formsAdapter.notifyDataSetChanged();
        }
    }
}
