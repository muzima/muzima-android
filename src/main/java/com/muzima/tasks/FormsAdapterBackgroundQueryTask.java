package com.muzima.tasks;

import android.os.AsyncTask;
import android.os.Build;
import android.widget.Toast;

import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.model.BaseForm;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.muzima.adapters.ListAdapter.BackgroundListQueryTaskListener;

public abstract class FormsAdapterBackgroundQueryTask<T extends BaseForm> extends AsyncTask<Void, Void, List<T>> {

    protected WeakReference<FormsAdapter> adapterWeakReference;

    public FormsAdapterBackgroundQueryTask(FormsAdapter adapter) {
        adapterWeakReference = new WeakReference<FormsAdapter>(adapter);
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
            Toast.makeText(adapterWeakReference.get().getContext(), "Could not load forms", Toast.LENGTH_SHORT).show();
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

    protected void changeDataSet(List<T> forms) {
        if (adapterWeakReference.get() != null) {
            FormsAdapter formsAdapter = adapterWeakReference.get();
            if (forms == null) {
                Toast.makeText(formsAdapter.getContext(), "Something went wrong while fetching forms from local repo", Toast.LENGTH_SHORT).show();
                return;
            }
            formsAdapter.clear();
            formsAdapter.addAll(forms);
            formsAdapter.notifyDataSetChanged();
        }
    }
}
