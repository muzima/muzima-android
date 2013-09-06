package com.muzima.tasks;

import android.os.AsyncTask;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.api.model.Form;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.muzima.adapters.ListAdapter.BackgroundListQueryTaskListener;

public class FormsAdapterBackgroundQueryTask extends AsyncTask<Void, Void, List<Form>> {

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
    protected List<Form> doInBackground(Void... params) {
        return null;
    }

    @Override
    protected void onPostExecute(List<Form> forms) {
        changeDataSet(forms);
        if (adapterWeakReference.get() != null) {
            FormsAdapter formsAdapter = adapterWeakReference.get();
            BackgroundListQueryTaskListener backgroundListQueryTaskListener = formsAdapter.getBackgroundListQueryTaskListener();
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskFinish();
            }
        }
    }

    protected void changeDataSet(List<Form> forms) {
        if (adapterWeakReference.get() != null) {
            FormsAdapter formsAdapter = adapterWeakReference.get();
            if (forms == null) {
                Toast.makeText(formsAdapter.getContext(), "Something went wrong while fetching forms from local repo", Toast.LENGTH_SHORT).show();
                return;
            }
            formsAdapter.clear();
            for (Form form : forms) {
                formsAdapter.add(form);
            }
            formsAdapter.notifyDataSetChanged();
        }
    }
}
