package com.muzima.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.MuzimaGeneratedReport;
import com.muzima.api.model.Patient;
import com.muzima.controller.MuzimaGeneratedReportController;
import com.muzima.view.reports.PatientReportWebActivity;

import java.net.URL;
import java.util.List;

/**
 * Created by dileka on 8/2/18.
 */
public class MuzimaGeneratedReportDownloadTask extends AsyncTask<Patient, Integer, List<MuzimaGeneratedReport>> {
    
    
    protected void onProgressUpdate(Integer... progress) {
    }
    
    @Override
    protected List<MuzimaGeneratedReport> doInBackground(Patient... patients) {
        
        MuzimaApplication muzimaApplication = new MuzimaApplication();
        MuzimaGeneratedReportController muzimaGeneratedReportController = muzimaApplication.getMuzimaGeneratedReportController();
    
        List<MuzimaGeneratedReport> muzimaGeneratedReports = null;
        try {
            muzimaGeneratedReports = muzimaGeneratedReportController.getAllMuzimaGeneratedReportsByPatientUuid(patients[0].getUuid());
        }
        catch (MuzimaGeneratedReportController.MuzimaGeneratedReportException e) {
            e.printStackTrace();
        }
        if(muzimaGeneratedReports.size()==0) {
            try {
                muzimaGeneratedReports = muzimaGeneratedReportController.downloadLastPriorityMuzimaGeneratedReportByPatientUuid(patients[0].getUuid());
                muzimaGeneratedReportController.saveAllMuzimaGeneratedReports(muzimaGeneratedReports);
            }
            catch (MuzimaGeneratedReportController.MuzimaGeneratedReportSaveException e) {
                e.printStackTrace();
            }
            catch (MuzimaGeneratedReportController.MuzimaGeneratedReportDownloadException e) {
                e.printStackTrace();
            }
        }
    
        return muzimaGeneratedReports;
    }
    
    protected void onPostExecute(List<MuzimaGeneratedReport> result) {
    
        MuzimaApplication muzimaApplication = new MuzimaApplication();
        
        Intent intent = new Intent(muzimaApplication.getApplicationContext(), PatientReportWebActivity.class);
    
        intent.putExtra("url", "http://www.cricinfo.com");
    
        Toast.makeText(muzimaApplication.getApplicationContext(), "77777777777777777", Toast.LENGTH_LONG).show();
        muzimaApplication.startActivity(intent);
    }
    
    
}
