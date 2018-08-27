/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.adapters.forms;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.muzima.R;
import com.muzima.controller.FormController;
import com.muzima.model.DownloadedForm;
import com.muzima.model.collections.DownloadedForms;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

import java.util.List;

/**
 * Responsible to list all the downloaded forms.
 */
public class DownloadedFormsAdapter extends FormsAdapter<DownloadedForm> {

    public DownloadedFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = super.getView(position, convertView, parent);

        hideTagScroller(convertView);
        return convertView;
    }

    private void hideTagScroller(View convertView) {
        convertView.findViewById(R.id.tags_scroller).setVisibility(View.GONE);
    }

    public void clearSelectedForms() {
        notifyDataSetChanged();
    }

    @Override
    protected int getFormItemLayout() {
        return R.layout.item_forms_list_selectable;
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    /**
     * Responsible to fetch all the
     */
    class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<DownloadedForm> {

        BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected List<DownloadedForm> doInBackground(Void... params) {
            DownloadedForms downloadedForms = null;
            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    downloadedForms = formsAdapter.getFormController().getAllDownloadedForms();
                    Log.i(getClass().getSimpleName(), "#Forms with templates: " + downloadedForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(getClass().getSimpleName(), "Exception occurred while fetching local forms ", e);
                }
            }
            return downloadedForms;
        }
    }
}
