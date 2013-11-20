package com.muzima.view.forms;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import com.muzima.utils.barcode.IntentIntegrator;

public class BarCodeComponent {


    private final Activity activity;
    private String fieldName;

    public BarCodeComponent(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void startBarCodeIntent(String fieldName) {
        this.fieldName = fieldName;
        IntentIntegrator intentIntegrator = new IntentIntegrator(activity);
        intentIntegrator.initiateScan();
    }

    public String getFieldName() {
        return fieldName;
    }
}
