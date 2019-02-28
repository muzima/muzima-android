/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.progressdialog;

import android.app.Activity;
import android.app.ProgressDialog;
import android.webkit.JavascriptInterface;
import com.muzima.R;

public class MuzimaProgressDialog {
    private final ProgressDialog dialog;

    public MuzimaProgressDialog(Activity activity) {
        this(new ProgressDialog(activity, R.style.alertDialogTheme));
    }

    MuzimaProgressDialog(ProgressDialog dialog) {
        this.dialog = dialog;
        this.dialog.setCancelable(false);
    }

    @JavascriptInterface
    public void show(String title) {
        dialog.setTitle(title);
        dialog.setMessage(dialog.getContext().getResources().getString(R.string.general_progress_message));
        dialog.show();
    }

    @JavascriptInterface
    public void updateMessage(String message) {
        dialog.setMessage(message);
    }

    @JavascriptInterface
    public void dismiss() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}