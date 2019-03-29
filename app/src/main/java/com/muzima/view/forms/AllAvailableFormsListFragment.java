/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.AllAvailableFormsAdapter;
import com.muzima.api.model.APIName;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.FormWithData;
import com.muzima.model.collections.CompleteFormsWithPatientData;
import com.muzima.model.collections.IncompleteFormsWithPatientData;
import com.muzima.service.MuzimaSyncService;
import com.muzima.utils.DateUtils;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.location.LocationListActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class AllAvailableFormsListFragment extends FormsListFragment {
    private ActionMode actionMode;
    private boolean actionModeActive = false;
    private OnTemplateDownloadComplete templateDownloadCompleteListener;
    private TextView syncText;
    private boolean newFormsSyncInProgress;
    private Activity mActivity;

    public static AllAvailableFormsListFragment newInstance(FormController formController) {
        AllAvailableFormsListFragment f = new AllAvailableFormsListFragment();
        f.formController = formController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (listAdapter == null) {
            listAdapter = new AllAvailableFormsAdapter(mActivity, R.layout.item_forms_list_selectable, formController);
        }
        noDataMsg = mActivity.getResources().getString(R.string.info_forms_unavailable);
        noDataTip = mActivity.getResources().getString(R.string.hint_form_list_download);

        // this can happen on orientation change
        if (actionModeActive) {
            actionMode = mActivity.startActionMode(new NewFormsActionModeCallback());
            actionMode.setTitle(String.valueOf(getSelectedForms().size()));
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            mActivity =(Activity) context;
            }
    }


    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.layout_synced_list, container, false);
        syncText = view.findViewById(R.id.sync_text);
        updateSyncTime();
        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (!actionModeActive) {
            actionMode = mActivity.startActionMode(new NewFormsActionModeCallback());
            actionModeActive = true;
        }
        int numOfSelectedForms = getSelectedForms().size();
        if (numOfSelectedForms == 0 && actionModeActive) {
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numOfSelectedForms));
    }

    public void onFormTemplateDownloadFinish() {
        if (templateDownloadCompleteListener != null) {
            templateDownloadCompleteListener.onTemplateDownloadComplete();
        }
        listAdapter.reloadData();
    }

    public void onFormMetaDataDownloadFinish() {
        newFormsSyncInProgress = false;
        listAdapter.reloadData();
        updateSyncTime();
    }

    public void onFormMetaDataDownloadStart() {
        newFormsSyncInProgress = true;
    }

    public void endActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    /**
     * Check whether  any of selected forms has existing patient data
     */
    private boolean patientDataExistsWithSelectedForms() {
        try {
            IncompleteFormsWithPatientData incompleteForms = formController.getAllIncompleteFormsWithPatientData();
            CompleteFormsWithPatientData completeForms = formController.getAllCompleteFormsWithPatientData(mActivity.getApplicationContext());
            if (patientDataExistsWithSelectedForms(incompleteForms) || patientDataExistsWithSelectedForms(completeForms)) {
                return true;
            }
        } catch (FormController.FormFetchException e) {
            Log.i(getClass().getSimpleName(), "Error getting forms with patient data");
        }
        return false;
    }

    /**
     * Check whether  any of selected forms has existing patient data
     *
     * @param formWithData Existing patient data to check against
     * @return true if there's any match, otherwise return false
     */
    private boolean patientDataExistsWithSelectedForms(ArrayList<? extends FormWithData> formWithData) {
        List<String> selectedFormsUuids = getSelectedForms();
        Iterator<? extends FormWithData> incompleteFormsIterator = formWithData.iterator();
        if (incompleteFormsIterator.hasNext()) {
            FormWithData incompleteForm = incompleteFormsIterator.next();
            return selectedFormsUuids.contains(incompleteForm.getFormUuid());
        }
        return false;
    }

    private String[] getSelectedFormsArray() {
        List<String> selectedForms = getSelectedForms();
        String[] selectedFormUuids = new String[selectedForms.size()];
        return selectedForms.toArray(selectedFormUuids);
    }

    private void updateSyncTime() {
        try {
            LastSyncTimeService lastSyncTimeService = ((MuzimaApplication) this.mActivity.getApplicationContext()).getMuzimaContext().getLastSyncTimeService();//((MuzimaApplication)getApplicationContext()).getMuzimaContext().getLastSyncTimeService();
            Date lastSyncedTime = lastSyncTimeService.getLastSyncTimeFor(APIName.DOWNLOAD_FORMS);
            String lastSyncedMsg = mActivity.getString(R.string.info_last_sync_unavailable);
            if (lastSyncedTime != null) {
                lastSyncedMsg = getString(R.string.hint_last_synced, DateUtils.getFormattedDateTime(lastSyncedTime));
            }
            syncText.setText(lastSyncedMsg);
        } catch (IOException e) {
            Log.i(getClass().getSimpleName(), "Error getting forms last sync time");
        }
    }

    private List<String> getSelectedForms() {
        List<String> formUUIDs = new ArrayList<>();
        SparseBooleanArray checkedItemPositions = list.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.valueAt(i)) {
                formUUIDs.add(((AvailableForm) list.getItemAtPosition(checkedItemPositions.keyAt(i))).getFormUuid());
            }
        }
        return formUUIDs;
    }

    public interface OnTemplateDownloadComplete {
        void onTemplateDownloadComplete();
    }

    final class NewFormsActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mActivity.getMenuInflater().inflate(R.menu.actionmode_menu_download, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_download:
                    if (newFormsSyncInProgress) {
                        Toast.makeText(mActivity, R.string.error_sync_not_allowed, Toast.LENGTH_SHORT).show();
                        endActionMode();
                        break;
                    }

                    if (!NetworkUtils.isConnectedToNetwork(mActivity)) {
                        Toast.makeText(mActivity, R.string.error_local_connection_unavailable, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    if (patientDataExistsWithSelectedForms()) {
                        AlertDialog alertDialog = new AlertDialog.Builder(mActivity).create();
                        alertDialog.setMessage((mActivity.getApplicationContext())
                                        .getString(R.string.error_form_patient_data_exist)
                        );
                        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        });
                        alertDialog.show();
                        return true;
                    }

                    //syncAllFormTemplatesInBackgroundService();

                    final AsyncTask<Void, Void, int[]> asynTask = new AsyncTask<Void, Void, int[]>() {
                        @Override
                        protected void onPreExecute() {
                            Log.i(getClass().getSimpleName(), "Canceling timeout timer!");
                            ((MuzimaApplication) mActivity.getApplicationContext()).cancelTimer();
                            ((FormsActivity) mActivity).showProgressBar();
                        }

                        @Override
                        protected int[] doInBackground(Void... voids) {
                            return downloadFormTemplates();
                        }

                        @Override
                        protected void onPostExecute(int[] results) {
                            navigateToNextActivity();
                        }
                    };
                    setRunningBackgroundQueryTask(asynTask);
                    asynTask.execute();

                    endActionMode();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            ((AllAvailableFormsAdapter) listAdapter).clearSelectedForms();
        }
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(mActivity.getApplicationContext(), LocationListActivity.class);
        if(isAdded()) {
            startActivity(intent);
        }
        mActivity.finish();
    }
    private int[] downloadFormTemplates() {
        List<String> selectedFormIdsArray = getSelectedForms();
        MuzimaSyncService muzimaSyncService = ((MuzimaApplication) mActivity.getApplicationContext()).getMuzimaSyncService();
        return muzimaSyncService.downloadFormTemplatesAndRelatedMetadata(selectedFormIdsArray.toArray(new String[selectedFormIdsArray.size()]), true);
    }

    private void syncAllFormTemplatesInBackgroundService() {
        ((FormsActivity) mActivity).showProgressBar();
        new SyncFormTemplateIntent((FragmentActivity) mActivity, getSelectedFormsArray()).start();
    }

    public void setTemplateDownloadCompleteListener(OnTemplateDownloadComplete templateDownloadCompleteListener) {
        this.templateDownloadCompleteListener = templateDownloadCompleteListener;
    }
}
