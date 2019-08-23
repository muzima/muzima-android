/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.reports;

import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.adapters.reports.AllPatientReportsAdapter;
import com.muzima.controller.PatientReportController;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.CheckedLinearLayout;

public class AllPatientReportListFragment extends PatientReportListFragment {
    private ActionMode actionMode;
    private boolean actionModeActive = false;
    private OnPatientReportsDownloadListener patientReportsDownloadListener;
    private boolean reportsSyncInProgress;
    private static String patientUuid;

    public static AllPatientReportListFragment newInstance(PatientReportController patientReportController, String patientUuid) {
        AllPatientReportListFragment f = new AllPatientReportListFragment();
        f.patientReportController = patientReportController;
        AllPatientReportListFragment.patientUuid = patientUuid;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (listAdapter == null) {
            listAdapter = new AllPatientReportsAdapter(getActivity(), R.layout.item_cohorts_list, patientReportController, patientUuid);
        }
        noDataMsg = getActivity().getResources().getString(R.string.info_patient_reports_unavailable);
        noDataTip = getActivity().getResources().getString(R.string.hint_patient_report_list_download);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.layout_synced_list, container, false);
        return view;
    }

    @Override
    protected String getSuccessMsg(Integer[] status) {
        return getString(R.string.info_patient_reports_downloaded, status[1]);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        CheckedLinearLayout checkedLinearLayout = (CheckedLinearLayout) view;
        boolean isChecked = checkedLinearLayout.isChecked();
        if (!actionModeActive && isChecked) {
            actionMode = getActivity().startActionMode(new AllPatientReportsActionModeCallback());
            actionModeActive = true;
        }
        ((AllPatientReportsAdapter) listAdapter).onListItemClick(position);
        int numberOfPatientReports = ((AllPatientReportsAdapter) listAdapter).numberOfPatientReports();
        if (numberOfPatientReports == 0 && actionModeActive) {
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numberOfPatientReports));
    }

    public void setPatientReportsDownloadListener(OnPatientReportsDownloadListener patientReportsDownloadListener) {
        this.patientReportsDownloadListener = patientReportsDownloadListener;
    }

    public void onPatientReportsDownloadFinish() {
        reportsSyncInProgress = false;
        listAdapter.reloadData();
        listAdapter.notifyDataSetChanged();
        if (patientReportsDownloadListener != null) {
            patientReportsDownloadListener.onPatientReportsDownloadComplete();
        }
    }

    public void onSelectedReportDownloadFinish() {
        listAdapter.reloadData();
        listAdapter.notifyDataSetChanged();
        if (patientReportsDownloadListener != null) {
            patientReportsDownloadListener.onPatientReportsDownloadComplete();
        }
    }

    public void onPatientReportsDownloadStart() {
        reportsSyncInProgress = true;
    }

    final class AllPatientReportsActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            getActivity().getMenuInflater().inflate(R.menu.actionmode_menu_download, menu);
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
                    if (reportsSyncInProgress) {
                        Toast.makeText(getActivity(), R.string.error_sync_not_allowed, Toast.LENGTH_SHORT).show();
                        endActionMode();
                        break;
                    }

                    if (!NetworkUtils.isConnectedToNetwork(getActivity())) {
                        Toast.makeText(getActivity(), R.string.error_local_connection_unavailable, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    downloadSelectedReportdInBackgroundService();

                    endActionMode();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            ((AllPatientReportsAdapter) listAdapter).clearSelectedReports();
            unselectAllItems(list);
        }
    }

    public void endActionMode() {
        if (this.actionMode != null) {
            this.actionMode.finish();
        }
    }

    private void downloadSelectedReportdInBackgroundService() {
        ((PatientReportActivity) getActivity()).showProgressBar();
        new DownloadPatientReportsIntent(getActivity(), ((AllPatientReportsAdapter) listAdapter).getSelectedPatientReportsArray()).start();
    }

    public interface OnPatientReportsDownloadListener {
        void onPatientReportsDownloadComplete();
    }
}
