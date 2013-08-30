package com.muzima.view.forms;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Form;
import com.muzima.api.model.FormData;
import com.muzima.api.model.FormTemplate;
import com.muzima.controller.FormController;

import static android.webkit.ConsoleMessage.MessageLevel.ERROR;
import static com.muzima.controller.FormController.FormFetchException;
import static java.text.MessageFormat.format;

public class FormWebViewActivity extends SherlockFragmentActivity {
    private static final String TAG = "FormWebViewActivity";
    public static final String FORM_UUID = "formId";
    public static final String FORM_DATA_UUID = "formId";
    public static final String FORM_INSTANCE = "formInstance";
    public static final String REPOSITORY = "formDataRepositoryContext";
    public static final String ZIGGY_FILE_LOADER = "ziggyFileLoader";


    private WebView webView;
    private String formDataUuid;
    private Form form;
    private FormTemplate formTemplate;
    private ProgressDialog progressDialog;
    private FormData formData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_webview);
        progressDialogInitialization();
        try {
            setupFormData();
            setupWebView();
        } catch (FormFetchException e) {
            Log.e(TAG, e.getMessage());
            finish();
        } catch (FormController.FormDataFetchException e) {
            Log.e(TAG, e.getMessage());
            finish();
        }
    }

    private void setupFormData() throws FormFetchException, FormController.FormDataFetchException {
        Intent intent = getIntent();
        String formId = intent.getStringExtra(FORM_UUID);
        formDataUuid = intent.getStringExtra(FORM_DATA_UUID);
        FormController formController = ((MuzimaApplication) getApplication()).getFormController();
        form = formController.getFormByUuid(formId);
        formTemplate = formController.getFormTemplateByUuid(formId);
        formData = formController.getFormDataByUuid(formDataUuid);
    }

    private void setupWebView() {
        webView = (WebView) findViewById(R.id.webView);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                FormWebViewActivity.this.setProgress(progress * 1000);

                if (progress == 100 && progressDialog.isShowing())
                    progressDialog.dismiss();
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String message = format("Javascript Log. Message: {0}, lineNumber: {1}, sourceId, {2}", consoleMessage.message(),
                        consoleMessage.lineNumber(), consoleMessage.sourceId());

                if (consoleMessage.messageLevel() == ERROR) {
                    Log.e(TAG, message);
                } else {
                    Log.d(TAG, message);
                }
                return true;
            }
        });

        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        FormInstance formInstance = new FormInstance(form, formTemplate);
        webView.addJavascriptInterface(formInstance, FORM_INSTANCE);
        FormController formController = ((MuzimaApplication) getApplication()).getFormController();
        webView.addJavascriptInterface(new FormDataRepository(formController, formData), REPOSITORY);
        webView.addJavascriptInterface(new ZiggyFileLoader("www/ziggy", "www/form", getApplicationContext().getAssets(), formInstance.getModelJson()), ZIGGY_FILE_LOADER);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.loadUrl("file:///android_asset/www/enketo/template.html");
    }

    private void progressDialogInitialization() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Loading ...");
        progressDialog.setMessage("Please wait");
        progressDialog.show();
    }
}
