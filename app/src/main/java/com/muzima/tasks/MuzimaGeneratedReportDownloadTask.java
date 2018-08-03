package com.muzima.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;
import com.muzima.MuzimaApplication;

import com.muzima.api.model.MuzimaGeneratedReport;
import com.muzima.api.model.Patient;
import com.muzima.controller.MuzimaGeneratedReportController;
import com.muzima.util.MuzimaLogger;
import com.muzima.view.reports.PatientReportWebActivity;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by dileka on 8/2/18.
 */
public class MuzimaGeneratedReportDownloadTask extends AsyncTask<Patient, Integer, List<MuzimaGeneratedReport>> {
    
    Context context;
    Logger logger;
    
    public MuzimaGeneratedReportDownloadTask(Context applicationContext) {
        context =applicationContext;
        logger =Logger.getLogger(this.getClass().getName());
    }
    

    protected void onProgressUpdate(Integer... progress) {
    }
    
    @Override
    protected List<MuzimaGeneratedReport> doInBackground(Patient... patients) {
        
        logger.warning("ttttttttttttttttttttttttttttttttttttttttttttttttttttttttt"+patients[0].getUuid());
    
        MuzimaGeneratedReportController muzimaGeneratedReportController = ((MuzimaApplication) context.getApplicationContext()).getMuzimaGeneratedReportController();
    
        List<MuzimaGeneratedReport> muzimaGeneratedReports = null;
        try {
            muzimaGeneratedReports = muzimaGeneratedReportController
                    .getAllMuzimaGeneratedReportsByPatientUuid(patients[0].getUuid());
            logger.warning("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv"+muzimaGeneratedReports.size());
        }
        catch (MuzimaGeneratedReportController.MuzimaGeneratedReportException e) {
            e.printStackTrace();
            logger.warning("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+e);
        }
        if (muzimaGeneratedReports.size() == 0) {
            logger.warning("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww"+muzimaGeneratedReports.size());
            try {
                muzimaGeneratedReports = muzimaGeneratedReportController
                        .downloadLastPriorityMuzimaGeneratedReportByPatientUuid(patients[0].getUuid());
                muzimaGeneratedReportController.saveAllMuzimaGeneratedReports(muzimaGeneratedReports);
                logger.warning("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"+muzimaGeneratedReports.size());
            }
            catch (MuzimaGeneratedReportController.MuzimaGeneratedReportSaveException e) {
                e.printStackTrace();
                logger.warning("yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy"+e);
            }
            catch (MuzimaGeneratedReportController.MuzimaGeneratedReportDownloadException e) {
                e.printStackTrace();
                logger.warning("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"+e);
            }
        }
        
        return muzimaGeneratedReports;
    }
    
    protected void onPostExecute(List<MuzimaGeneratedReport> result) {
        
        Intent intent = new Intent(context.getApplicationContext(), PatientReportWebActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if(result.size()>0){
            intent.putExtra("url", "http://www.cricinfo.com");
        }
        else{
            intent.putExtra("url", "http://www.google.com");
        }
        
        context.startActivity(intent);
    }
    
}
