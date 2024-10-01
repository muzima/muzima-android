/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.progressdialog;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.webkit.JavascriptInterface;
import com.muzima.R;
import com.muzima.utils.MuzimaPreferences;

public class MuzimaProgressDialog {
    private final ProgressDialog dialog;

    public MuzimaProgressDialog(Activity activity) {
        this(new ProgressDialog(activity, getStyle(activity)));
    }

    private static int getStyle(Activity activity) {
        boolean lightMode = MuzimaPreferences.getBooleanPreference(activity, activity.getResources().getString(R.string.preference_light_mode), false);

        if (lightMode) {
            return R.style.alertDialogThemeLight;
        } else {
            return R.style.alertDialogTheme;
        }
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
        try {
            if ((dialog != null) && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (final IllegalArgumentException e) {
            Log.e(getClass().getSimpleName(),"An Illegal Argument Exception  occurred while dismissing the dialog ",e);
        } catch (final Exception e) {
            Log.e(getClass().getSimpleName(),"An Exception  occurred while dismissing the dialog ",e);
        }
    }
}
