package com.muzima.messaging.utils;

import android.app.AlertDialog;
import android.content.Context;

import com.muzima.R;

public class Dialogs {
    public static void showAlertDialog(Context context, String title, String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIconAttribute(R.attr.dialog_alert_icon);
        dialog.setPositiveButton(android.R.string.ok, null);
        dialog.show();
    }

    public static void showInfoDialog(Context context, String title, String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIconAttribute(R.attr.dialog_info_icon);
        dialog.setPositiveButton(android.R.string.ok, null);
        dialog.show();
    }
}
