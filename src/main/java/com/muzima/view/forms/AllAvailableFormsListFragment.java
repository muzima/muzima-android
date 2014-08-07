/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;
import com.muzima.adapters.forms.AllAvailableFormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AllAvailableFormsListFragment extends FormsListFragment {
    private static final String TAG = "AllAvailableFormsListFragment";

    public static final String FORMS_METADATA_LAST_SYNCED_TIME = "formsMetadataSyncedTime";
    public static final long NOT_SYNCED_TIME = -1;

    private ActionMode actionMode;
    private boolean actionModeActive = false;
    private OnTemplateDownloadComplete templateDownloadCompleteListener;
    private TextView syncText;
    private boolean newFormsSyncInProgress;

    public static AllAvailableFormsListFragment newInstance(FormController formController) {
        AllAvailableFormsListFragment f = new AllAvailableFormsListFragment();
        f.formController = formController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (listAdapter == null) {
            listAdapter = new AllAvailableFormsAdapter(getActivity(), R.layout.item_forms_list_selectable, formController);
        }
        noDataMsg = getActivity().getResources().getString(R.string.no_new_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_new_form_tip);

        // this can happen on orientation change
        if (actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new NewFormsActionModeCallback());
            actionMode.setTitle(String.valueOf(getSelectedForms().size()));
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.layout_synced_list, container, false);
        syncText = (TextView) view.findViewById(R.id.sync_text);
        updateSyncTime();
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (!actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new NewFormsActionModeCallback());
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

    public final class NewFormsActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            getSherlockActivity().getSupportMenuInflater().inflate(R.menu.actionmode_menu_download, menu);
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
                        Toast.makeText(getActivity(), "Action not allowed while sync is in progress", Toast.LENGTH_SHORT).show();
                        endActionMode();
                        break;
                    }

                    if (!NetworkUtils.isConnectedToNetwork(getActivity())) {
                        Toast.makeText(getActivity(), "No connection found, please connect your device and try again", Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    syncAllFormTemplatesInBackgroundService();

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

    public void endActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private void syncAllFormTemplatesInBackgroundService() {
        ((FormsActivity) getActivity()).showProgressBar();
        new SyncFormTemplateIntent(getActivity(), getSelectedFormsArray()).start();
    }

    public void setTemplateDownloadCompleteListener(OnTemplateDownloadComplete templateDownloadCompleteListener) {
        this.templateDownloadCompleteListener = templateDownloadCompleteListener;
    }

    public interface OnTemplateDownloadComplete {
        public void onTemplateDownloadComplete();
    }

    private String[] getSelectedFormsArray() {
        List<String> selectedForms = getSelectedForms();
        String[] selectedFormUuids = new String[selectedForms.size()];
        return selectedForms.toArray(selectedFormUuids);
    }

    private void updateSyncTime() {
        SharedPreferences pref = getActivity().getSharedPreferences(Constants.SYNC_PREF, Context.MODE_PRIVATE);
        long lastSyncedTime = pref.getLong(FORMS_METADATA_LAST_SYNCED_TIME, NOT_SYNCED_TIME);
        String lastSyncedMsg = "Not synced yet";
        if (lastSyncedTime != NOT_SYNCED_TIME) {
            lastSyncedMsg = "Last synced on: " + DateUtils.getFormattedDateTime(new Date(lastSyncedTime));
        }
        Log.d(TAG, lastSyncedMsg);
        syncText.setText(lastSyncedMsg);
    }

    private List<String> getSelectedForms(){
        List<String> formUUIDs = new ArrayList<String>();
        SparseBooleanArray checkedItemPositions = list.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.valueAt(i)) {
                formUUIDs.add(((AvailableForm) list.getItemAtPosition(checkedItemPositions.keyAt(i))).getFormUuid());
            }
        }
        return formUUIDs;
    }
}
