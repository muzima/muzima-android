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
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.api.model.PatientReport;
import com.muzima.controller.PatientReportController;
import com.muzima.tasks.MuzimaAsyncTask;

import java.util.List;

/**
 * Responsible to populate downloaded patient reports fetched from DB in the DownloadedPatientReportsListFragment page.
 */
public class DownloadedPatientReportAdapter extends ReportsAdapter {

    public DownloadedPatientReportAdapter(Context context, int textViewResourceId, PatientReportController patientReportController, String patientUuid) {
        super(context, textViewResourceId, patientReportController, patientUuid);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        convertView = super.getView(position,convertView,parent);
        ViewHolder holder = (ViewHolder) convertView.getTag();

        PatientReport patientReport = getItem(position);
        holder.setTextToName(patientReport.getName());
        holder.setDefaultTextColor();
        return convertView;
    }

    @Override
    public void reloadData() {
        new DownloadedPatientReportAdapter.BackgroundQueryTask().execute();
    }

    class BackgroundQueryTask extends MuzimaAsyncTask<Void, Void, List<PatientReport>> {

        @Override
        protected void onPreExecute() {
            if(backgroundListQueryTaskListener != null){
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected List<PatientReport> doInBackground(Void... voids) {
            List<PatientReport> patientReports = null;
            try {
                patientReports = patientReportController.getPatientReportsByPatientUuid(patientUuid);
                Log.i(getClass().getSimpleName(), "#Patient reports: " + patientReports.size());
            } catch (PatientReportController.PatientReportException e) {
                Log.e(getClass().getSimpleName(), "Exception occurred while fetching downloaded patient reports ", e);
            }

            return patientReports;
        }

        @Override
        protected void onPostExecute(List<PatientReport> patientReports) {

            if(patientReports == null) {
                Toast.makeText(getContext(), getContext().getString(R.string.error_patient_report_fetch), Toast.LENGTH_SHORT).show();
                return;
            }
            clear();
            addAll(patientReports);
            notifyDataSetChanged();

            if(backgroundListQueryTaskListener != null){
                backgroundListQueryTaskListener.onQueryTaskFinish();
            }
        }

        @Override
        protected void onBackgroundError(Exception e) {

        }
    }
}
