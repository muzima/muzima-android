/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.reports;

import android.content.Context;
import android.util.Log;
import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.collections.AvailableForms;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

import java.util.List;

public class AvailableReportsAdapter extends FormsAdapter<AvailableForm> {

    public AvailableReportsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
    }

    @Override
    public void reloadData() {
        new AvailableReportsAdapter.BackgroundQueryTask(this).execute();
    }

    protected class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<AvailableForm> {

        BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }
        @Override
        protected void onPreExecute() {
            backgroundListQueryTaskListener.onQueryTaskStarted();
            AvailableReportsAdapter.this.clear();
        }
        @Override
        protected List<AvailableForm> doInBackground(Void... params) {
            AvailableForms reportTemplates = new AvailableForms();
            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    reportTemplates = formsAdapter.getFormController().getProviderReports();
                } catch (FormController.FormFetchException e) {
                    Log.w(getClass().getSimpleName(), "Exception occurred while fetching local provider reports ", e);
                }
            }
            return reportTemplates;
        }
        @Override
        protected void onPostExecute(List<AvailableForm> reportTemplates){
            if(reportTemplates.size() > 0){
                clear();
                addAll(reportTemplates);
                notifyDataSetChanged();
            } else {
                backgroundListQueryTaskListener.onQueryTaskFinish();
            }
        }

        @Override
        protected void onBackgroundError(Exception e) {

        }
    }
}
