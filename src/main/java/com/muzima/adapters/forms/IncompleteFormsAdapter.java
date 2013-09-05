package com.muzima.adapters.forms;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.muzima.api.model.Form;
import com.muzima.controller.FormController;
import com.muzima.tasks.QueryTask;

import java.util.ArrayList;
import java.util.List;

public class IncompleteFormsAdapter extends FormsAdapter {
    private static final String TAG = "NewFormsAdapter";

    public IncompleteFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(backgroundListQueryTaskListener).execute();
    }

    public class BackgroundQueryTask extends QueryTask {

        public BackgroundQueryTask(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
            super(backgroundListQueryTaskListener);
        }

        @Override
        protected List<Form> doInBackground(Void... voids) {
            List<Form> downloadedForms = null;
            try {
                downloadedForms = formController.getAllIncompleteForms();

                Log.i(TAG, "#Forms with templates: " + downloadedForms.size());
            } catch (FormController.FormFetchException e) {
                Log.w(TAG, "Exception occurred while fetching local forms " + e);
            }
            return downloadedForms;
        }

        @Override
        protected void onPostExecute(List<Form> forms) {
            if(forms == null){
                Toast.makeText(getContext(), "Something went wrong while fetching forms from local repo", Toast.LENGTH_SHORT).show();
                return;
            }

            IncompleteFormsAdapter.this.clear();
            for (Form form : forms) {
                add(form);
            }
            notifyDataSetChanged();

            super.onPostExecute(forms);
        }
    }
}
