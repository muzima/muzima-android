/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.app.Activity;
import android.app.ProgressDialog;
import android.webkit.JavascriptInterface;

public class MuzimaProgressDialog {
    private ProgressDialog dialog;

    public MuzimaProgressDialog(Activity activity) {
        this(new ProgressDialog(activity));
    }

    MuzimaProgressDialog(ProgressDialog dialog) {
        this.dialog = dialog;
        this.dialog.setCancelable(false);
    }

    @JavascriptInterface
    public void show(String title) {
        dialog.setTitle(title);
        dialog.setMessage("This might take a while");
        dialog.show();
    }

    @JavascriptInterface
    public void dismiss() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
