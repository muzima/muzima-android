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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.MuzimaGeneratedReport;
import com.muzima.api.model.Patient;
import com.muzima.controller.MuzimaGeneratedReportController;
import com.muzima.view.BaseActivity;

import java.util.List;

import static com.muzima.view.patients.PatientSummaryActivity.PATIENT;

public class PatientReportWebActivity extends BaseActivity {
    
    private static final String TAG = "PatientReportWebActivity";
    
    private Patient patient;
    
    private WebView myWebView;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_report);
        patient = (Patient) getIntent().getSerializableExtra(PATIENT);
        
        myWebView = (WebView) findViewById(R.id.activity_main_webview);
        myWebView.setWebViewClient(new WebViewClient());
        MuzimaGeneratedReportController muzimaGeneratedReportController = ((MuzimaApplication) getApplicationContext())
                .getMuzimaGeneratedReportController();
        
        try {
            List<MuzimaGeneratedReport> muzimaGeneratedReportList = muzimaGeneratedReportController
                    .getAllMuzimaGeneratedReportsByPatientUuid(patient.getUuid());
            
            myWebView.loadDataWithBaseURL(null, muzimaGeneratedReportList.get(0).getReportJson(), "text/html; charset=utf-8",
                    "UTF-8", null);
        }
        catch (MuzimaGeneratedReportController.MuzimaGeneratedReportException e) {
            e.printStackTrace();
        }
        
    }
}


