/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.reports;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.MuzimaGeneratedReport;
import com.muzima.api.model.Patient;
import com.muzima.controller.MuzimaGeneratedReportController;
import com.muzima.view.BaseActivity;

import java.util.List;
import java.util.logging.Logger;

import static com.muzima.view.patients.PatientSummaryActivity.PATIENT;

public class PatientReportWebActivity extends BaseActivity {
    
    private static final String TAG = "PatientReportWebActivity";
    private Patient patient;
    private WebView myWebView;
    Logger logger;
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_reports);
        patient = (Patient) getIntent().getSerializableExtra(PATIENT);
        logger = Logger.getLogger(this.getClass().getName());
        logger.warning("888888888888888888888888888888"+patient.getUuid());
        Toast.makeText(getApplicationContext(), "111111111rrrrrrrrrrrrrrrrrrrrr", Toast.LENGTH_LONG).show();
    
    
    
    
    
    }
    public void showReport(View v) {
        Toast.makeText(getApplicationContext(), "22222222222222222rrrrrrrrrrrrrrrrrrrrr", Toast.LENGTH_LONG).show();
    
        logger.warning("222222222222222222222277777777777777777"+patient.getUuid());
    
        String url = getIntent().getStringExtra("url");
        myWebView = (WebView) findViewById(R.id.activity_main_webview);
        myWebView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //myWebView.loadUrl(url);
        MuzimaGeneratedReportController muzimaGeneratedReportController = ((MuzimaApplication) getApplicationContext()).getMuzimaGeneratedReportController();
        
      try {
            List<MuzimaGeneratedReport> muzimaGeneratedReportList = muzimaGeneratedReportController.getAllMuzimaGeneratedReportsByPatientUuid(patient.getUuid());
            
            logger.warning("55555555"+muzimaGeneratedReportList.size());
          
            MuzimaGeneratedReport muzimaGeneratedReport = muzimaGeneratedReportController.getLastPriorityMuzimaGeneratedReport("3f36e28a-3cc2-4d31-b287-f4e323dc7278");
            Toast.makeText(getApplicationContext(), "7777777777rrrrrrrrrrrrrrrrrrrr" + muzimaGeneratedReport.getReportJson(), Toast.LENGTH_LONG).show();
            logger.warning("9999999999999999999999"+muzimaGeneratedReport.getPatientUuid());
            logger.warning("qqqqqqqqqqqqqqqqqqqqqqq"+muzimaGeneratedReport.getReportJson());
            byte[] b = muzimaGeneratedReportList.get(0).getReportJson().getBytes();
            String s = new String(b);
            logger.warning("ppppppppppppppppppppp"+s);
           Toast.makeText(getApplicationContext(), "9999999999999rrrrrrrrrrrrrrrrrrrr" + s, Toast.LENGTH_LONG).show();
           myWebView.loadDataWithBaseURL(null,s,"text/html; charset=utf-8", "UTF-8",null);
        }
        catch (MuzimaGeneratedReportController.MuzimaGeneratedReportException e) {
            e.printStackTrace();
        }
      catch (MuzimaGeneratedReportController.MuzimaGeneratedReportFetchException e) {
          e.printStackTrace();
      }
        
       }
    
}


