/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import com.muzima.utils.barcode.BarCodeScannerIntentIntegrator;

class BarCodeComponent {


    private final Activity activity;
    private String fieldName;

    public BarCodeComponent(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void startBarCodeIntent(String fieldName) {
        this.fieldName = fieldName;
        BarCodeScannerIntentIntegrator barCodeScannerIntentIntegrator = new BarCodeScannerIntentIntegrator(activity);
        barCodeScannerIntentIntegrator.initiateScan();
    }

    public String getFieldName() {
        return fieldName;
    }
}
