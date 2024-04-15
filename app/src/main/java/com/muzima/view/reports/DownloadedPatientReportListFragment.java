/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.reports;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.reports.DownloadedPatientReportAdapter;
import com.muzima.controller.PatientReportController;

public class DownloadedPatientReportListFragment extends PatientReportListFragment implements AllPatientReportListFragment.OnPatientReportsDownloadListener {
    private static String patientUuid;

    public static DownloadedPatientReportListFragment newInstance(PatientReportController patientReportController, String patientUuid) {
        DownloadedPatientReportListFragment f = new DownloadedPatientReportListFragment();
        f.patientReportController = patientReportController;
        DownloadedPatientReportListFragment.patientUuid = patientUuid;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (listAdapter == null) {
            listAdapter = new DownloadedPatientReportAdapter(getActivity(), R.layout.item_notifications_list, patientReportController, patientUuid);
        }
        noDataMsg = getActivity().getResources().getString(R.string.info_patient_reports_unavailable);
        noDataTip = getActivity().getResources().getString(R.string.hint_patient_reports_sync);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser && isResumed()){
            logEvent("VIEW_CLIENT_DOWNLOADED_REPORTS", "{\"patientuuid\":\""+patientUuid+"\"}");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        String patientReportUuid = ((DownloadedPatientReportAdapter) listAdapter).getItem(position).getUuid();
        Intent intent = new Intent(getActivity(), PatientReportWebActivity.class);

        intent.putExtra(PatientReportWebActivity.PATIENT_REPORT_UUID, patientReportUuid);
        startActivity(intent);
    }

    @Override
    protected String getSuccessMsg(Integer[] status) {
        return "Downloaded " + status[2] + " reports for patient";
    }

    @Override
    public void onPatientReportsDownloadComplete() {
        reloadData();
    }
}
