package com.muzima.view.forms;

import android.app.Activity;
import android.app.ProgressDialog;
import android.webkit.JavascriptInterface;

public class FormProgressDialog {
    private ProgressDialog dialog;

    public FormProgressDialog(Activity activity) {
        this(new ProgressDialog(activity));
    }

    FormProgressDialog(ProgressDialog dialog) {
        this.dialog = dialog;
        this.dialog.setCancelable(false);
    }

    @JavascriptInterface
    public void show(String title) {
        dialog.setTitle(title);
        dialog.setMessage("Please wait");
        dialog.show();
    }

    @JavascriptInterface
    public void dismiss() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @JavascriptInterface
    public void hide() {
        if (dialog.isShowing()) {
            dialog.hide();
        }
    }
}
