
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

import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Provider;
import com.muzima.model.AvailableForm;
import com.muzima.controller.FormController;
import com.muzima.utils.javascriptinterface.FormDataJavascriptInterface;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.progressdialog.MuzimaProgressDialog;

import static android.webkit.ConsoleMessage.MessageLevel.ERROR;
import static java.text.MessageFormat.format;


public class ProviderReportViewActivity extends BroadcastListenerActivity {
    public static final String REPORT = "SelectedReport";
    public Provider provider;
    private MuzimaProgressDialog progressDialog;
    private FormTemplate reportTemplate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_webview);
        progressDialog = new MuzimaProgressDialog(this);
        FormController formController = ((MuzimaApplication) getApplicationContext()).getFormController();
        try {
            AvailableForm availableForm = (AvailableForm)getIntent().getSerializableExtra(REPORT);
            reportTemplate = formController.getFormTemplateByUuid(availableForm.getFormUuid());
        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(),"Could not obtain report template");
        }
        setupWebView();
    }

    private void setupWebView() {
        WebView webView;
        webView = findViewById(R.id.webView);
        webView.setWebChromeClient(createWebChromeClient( ));

        webView.getSettings( ).setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.getSettings( ).setJavaScriptEnabled(true);
        webView.getSettings( ).setDatabaseEnabled(true);
        webView.getSettings( ).setDomStorageEnabled(true);
        webView.getSettings( ).setBuiltInZoomControls(true);

        webView.addJavascriptInterface(new FormDataJavascriptInterface((MuzimaApplication) getApplicationContext()),
                "formDataInterface");
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        webView.loadDataWithBaseURL("file:///android_asset/www/forms/", prePopulateData( ),
                "text/html", "UTF-8", "");
    }

    private String prePopulateData() {
        if (reportTemplate != null) {
            return reportTemplate.getHtml();
        }
        return null;
    }

    private WebChromeClient createWebChromeClient() {
        return new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int progress) {
                ProviderReportViewActivity.this.setProgress(progress * 1000);
                if (progress == 100) {
                    progressDialog.dismiss( );
                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String message = format("Javascript Log. Message: {0}, lineNumber: {1}, sourceId, {2}", consoleMessage.message( ),
                        consoleMessage.lineNumber( ), consoleMessage.sourceId( ));
                if (consoleMessage.messageLevel( ) == ERROR) {
                    Log.e(getClass().getSimpleName(), message);
                } else {
                    Log.d(getClass().getSimpleName(), message);
                }
                return true;
            }
        };
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null) {
            progressDialog.dismiss( );
        }
        super.onDestroy( );
    }
}



