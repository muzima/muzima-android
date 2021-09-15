/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.app.Activity;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import com.muzima.view.barcode.BarcodeCaptureActivity;

class BarCodeComponent {


    private final Activity activity;
    private String fieldName;
    public static final int RC_BARCODE_CAPTURE = 9001;

    public BarCodeComponent(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void startBarCodeIntent(String fieldName) {
        this.fieldName = fieldName;
        Intent intent;
        intent = new Intent(activity, BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
        intent.putExtra(BarcodeCaptureActivity.UseFlash, false);

        startActivityForResult(intent);
    }


    private void startActivityForResult(Intent intent) {
        activity.startActivityForResult(intent, RC_BARCODE_CAPTURE);
    }

    public String getFieldName() {
        return fieldName;
    }
}
