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

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.PatientReport;
import com.muzima.controller.PatientReportController;
import com.muzima.view.BroadcastListenerActivity;

import static android.webkit.ConsoleMessage.MessageLevel.ERROR;
import static java.text.MessageFormat.format;

public class PatientReportWebActivity extends BroadcastListenerActivity {
    static final String PATIENT_REPORT_UUID = "SelectedPatientReportUUID";
    private PatientReport patientReport;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_webview);
        String patientReportUuid = getIntent().getStringExtra(PATIENT_REPORT_UUID);

        PatientReportController patientReportController = ((MuzimaApplication) getApplicationContext()).getPatientReportController();

        try {
            patientReport = patientReportController.getPatientReportByUuid(patientReportUuid);

            showReport();
        } catch (PatientReportController.PatientReportFetchException e) {
            Log.e(getClass().getSimpleName(), "Unable to load a previously available report", e);
        }
        logEvent("VIEW_PATIENT_REPORT", "{\"patientuuid\":\""+patientReport.getPatientUuid()+"\"" +
                ",\"patientuuid\":\""+patientReport.getUuid()+"\"}");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void showReport() {
        WebView webView = findViewById(R.id.webView);
        webView.setWebChromeClient(createWebChromeClient());
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setWebContentsDebuggingEnabled(true);
        }

        webView.loadDataWithBaseURL(
                "file:///android_asset/www/forms/",
                patientReport.getReportJson(),
                "text/html; charset=utf-8",
                "UTF-8",
                null);
    }

    private WebChromeClient createWebChromeClient() {
        return new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String message = format("Javascript Log. Message: {0}, lineNumber: {1}, sourceId, {2}", consoleMessage.message(),
                        consoleMessage.lineNumber(), consoleMessage.sourceId());
                if (consoleMessage.messageLevel() == ERROR) {
                    Log.e(getClass().getSimpleName(), message);
                } else {
                    Log.d(getClass().getSimpleName(), message);
                }
                return true;
            }
        };
    }
}


