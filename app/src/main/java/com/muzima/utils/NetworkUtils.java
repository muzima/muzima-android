/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.muzima.utils.Constants.SERVER_CONNECTIVITY_STATUS;

public class NetworkUtils {
    public static boolean isConnectedToNetwork(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public static SERVER_CONNECTIVITY_STATUS getServerStatus(Context context, String server){
        if (!NetworkUtils.isConnectedToNetwork(context)) {
            return Constants.SERVER_CONNECTIVITY_STATUS.INTERNET_FAILURE;
        } else {
            if(com.muzima.util.NetworkUtils.isAddressReachable(server)){
                return Constants.SERVER_CONNECTIVITY_STATUS.SERVER_ONLINE;
            } else {
                return Constants.SERVER_CONNECTIVITY_STATUS.SERVER_OFFLINE;
            }
        }
    }
}
