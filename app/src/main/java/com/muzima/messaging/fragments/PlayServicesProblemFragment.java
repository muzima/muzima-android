package com.muzima.messaging.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.muzima.R;

public class PlayServicesProblemFragment extends DialogFragment {

    @Override
    public @NonNull
    Dialog onCreateDialog(@NonNull Bundle bundle) {
        int    code   = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(code, getActivity(), 9111);

        if (dialog == null) {
            return new AlertDialog.Builder(getActivity())
                    .setNegativeButton(android.R.string.ok, null)
                    .setMessage(R.string.warning_google_play_problem)
                    .create();
        } else {
            return dialog;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        finish();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        finish();
    }

    private void finish() {
        Activity activity = getActivity();
        if (activity != null) activity.finish();
    }
}
