package com.muzima.adapters.forms;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.muzima.controller.FormController;
import com.muzima.model.CompleteFormWithPatientData;
import com.muzima.model.FormWithData;
import com.muzima.model.collections.CompleteFormsWithPatientData;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;
import com.muzima.view.forms.FormViewIntent;
import com.muzima.view.forms.FormsActivity;

import java.util.List;

public class CompleteFormsAdapter extends SectionedFormsAdapter<CompleteFormWithPatientData>{
    private static final String TAG = "CompleteFormsAdapter";

    public CompleteFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.showViewDataButton();
        holder.viewDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FormWithData item = getItem(position);
                FragmentActivity activity = (FragmentActivity) getContext();
                FormViewIntent intent = new FormViewIntent(activity, item, item.getPatient());
                activity.startActivityForResult(intent, FormsActivity.FORM_VIEW_ACTIVITY_RESULT);
            }
        });
        return view;
    }

    public static class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<CompleteFormWithPatientData> {

        public BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected CompleteFormsWithPatientData doInBackground(Void... voids) {
            CompleteFormsWithPatientData completeForms = null;

            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    completeForms = formsAdapter.getFormController().getAllCompleteForms();
                    Log.i(TAG, "#Complete forms: " + completeForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms " + e);
                }
            }

            return completeForms;
        }

        @Override
        protected void onPostExecute(List<CompleteFormWithPatientData> forms) {
            if (adapterWeakReference.get() != null) {
                SectionedFormsAdapter formsAdapter = (SectionedFormsAdapter)adapterWeakReference.get();
                formsAdapter.setPatients(formsAdapter.buildPatientsList(forms));
                formsAdapter.sortFormsByPatientName(forms);
                notifyListener();
            }
        }
    }
}
