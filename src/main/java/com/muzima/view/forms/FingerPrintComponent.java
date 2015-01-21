/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.webkit.JavascriptInterface;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.utils.barcode.IntentIntegrator;

import java.util.List;

public class FingerPrintComponent {


    private final Activity activity;
    private String fieldName;

    public FingerPrintComponent(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void startFingerPrintIntent(String fieldName) {
        this.fieldName = fieldName;
        IntentIntegrator intentIntegrator = new IntentIntegrator(activity);
        intentIntegrator.initiateFingerPrintScan();
    }

    public String getFieldName() {
        return fieldName;
    }
}
