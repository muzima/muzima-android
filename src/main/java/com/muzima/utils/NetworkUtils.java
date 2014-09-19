/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.view.login.LoginActivity;

public class NetworkUtils {
    public static boolean isConnectedToNetwork(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }


    public static void checkAndExecuteInternetBasedOperation(Activity activity, Runnable retryAction, Runnable defaultAction) {
        if (!isConnectedToNetwork(activity)) {
            showNoInternetConnectivityAlert(activity, retryAction);
        } else {
            defaultAction.run();
        }
    }

    private static void showNoInternetConnectivityAlert(Activity activity, Runnable retryAction) {
        new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setIcon(activity.getResources().getDrawable(R.drawable.ic_warning))
                .setTitle(activity.getResources().getString(R.string.caution))
                .setMessage(activity.getResources().getString(R.string.no_internet_connectivity_message))
                .setPositiveButton(activity.getString(R.string.retry_button_label), positiveClickListener(retryAction))
                .setNegativeButton(activity.getString(R.string.exit_button_label), negativeClickListener(activity))
                .create()
                .show();

    }

    private static void launchLoginActivity(boolean isFirstLaunch, Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.putExtra(LoginActivity.isFirstLaunch, isFirstLaunch);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    private static DialogInterface.OnClickListener positiveClickListener(final Runnable retryAction) {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                retryAction.run();
            }
        };
    }

    private static DialogInterface.OnClickListener negativeClickListener(final Activity activity) {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((MuzimaApplication) activity.getApplication()).logOut();
                launchLoginActivity(false, activity);
            }
        };
    }
}
