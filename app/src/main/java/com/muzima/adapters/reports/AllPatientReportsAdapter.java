/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.adapters.reports;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.api.model.PatientReport;
import com.muzima.api.model.PatientReportHeader;
import com.muzima.controller.PatientReportController;
import com.muzima.tasks.MuzimaAsyncTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible to populate downloaded patient reports fetched from DB in the AllPatientReportListFragment page.
 */
public class AllPatientReportsAdapter extends ReportsAdapter {
    private final List<String> selectedReportsUuid;

    public AllPatientReportsAdapter(Context context, int textViewResourceId, PatientReportController patientReportController, String patientUuid) {
        super(context, textViewResourceId, patientReportController, patientUuid);
        selectedReportsUuid = new ArrayList<>();
    }

    @Override
    public void reloadData() {
        new LoadBackgroundQueryTask().execute();
    }

    public List<String> getSelectedPatientReports() {
        return selectedReportsUuid;
    }

    public String[] getSelectedPatientReportsArray() {
        return getSelectedPatientReports().toArray(new String[getSelectedPatientReports().size()]);
    }

    public void clearSelectedReports() {
        selectedReportsUuid.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        ViewHolder holder = (ViewHolder) view.getTag();
        PatientReport patientReport = getItem(position);

        if (patientReportController.isDownloaded(patientReport)) {
            holder.displayDownloadImage();
            holder.hidePendingUpdateImage();
            holder.setDefaultTextColor();
        } else {
            holder.hideDownloadImage();
            holder.hidePendingUpdateImage();
            holder.setDefaultTextColor();
        }
        highlightPatientReport(patientReport, view);
        return view;
    }

    private void highlightPatientReport(PatientReport report, View view) {
        if (selectedReportsUuid.contains(report.getUuid())) {
            view.setBackgroundResource(R.color.primary_blue);
        } else {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getContext().getTheme();
            theme.resolveAttribute(R.attr.primaryBackgroundColor, typedValue, true);
            view.setBackgroundResource(typedValue.resourceId);
        }
    }

    public void onListItemClick(int position) {
        PatientReport patientReport = getItem(position);
        if (!selectedReportsUuid.contains(patientReport.getUuid())) {
            selectedReportsUuid.add(patientReport.getUuid());
        } else if (selectedReportsUuid.contains(patientReport.getUuid())) {
            selectedReportsUuid.remove(patientReport.getUuid());
        }
        notifyDataSetChanged();
    }

    public int numberOfPatientReports() {
        return getSelectedPatientReports().size();
    }

    /**
     * Responsible to define contract to PatientReportsBackgroundQueryTask.
     */
    abstract class PatientReportsBackgroundQueryTask extends MuzimaAsyncTask<Void, Void, List<PatientReport>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected void onPostExecute(List<PatientReport> patientReports) {
            if (patientReports == null) {
                Toast.makeText(getContext(), getContext().getString(R.string.error_patient_report_fetch), Toast.LENGTH_SHORT).show();
                return;
            }

            clear();
            addAll(patientReports);
            notifyDataSetChanged();

            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskFinish();
            }
        }

        protected abstract List<PatientReport> doInBackground(Void... voids);
    }

    /**
     * Responsible to load patient Reports from database. Runs in Background.
     */
    protected class LoadBackgroundQueryTask extends PatientReportsBackgroundQueryTask {

        @Override
        protected List<PatientReport> doInBackground(Void... voids) {
            List<PatientReportHeader> patientReportHeaders;
            List<PatientReport> patientReports = new ArrayList<>();
            try {
                patientReportHeaders = patientReportController.getPatientReportHeadersByPatientUuid(patientUuid);
                for (PatientReportHeader header : patientReportHeaders) {
                    PatientReport report = new PatientReport(header.getUuid(), header.getName(), null);
                    patientReports.add(report);
                }
                Log.d(getClass().getSimpleName(), "#Retrieved " + patientReportHeaders.size() + " Patient report headers from Database.");
            } catch (PatientReportController.PatientReportException e) {
                Log.w(getClass().getSimpleName(), "Exception occurred while fetching local patient report headers ", e);
            }
            return patientReports;
        }

        @Override
        protected void onBackgroundError(Exception e) {

        }
    }
}
