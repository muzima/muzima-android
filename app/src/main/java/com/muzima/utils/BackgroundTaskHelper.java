/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils;

import android.os.AsyncTask;

public class BackgroundTaskHelper {
    public static <P, T extends AsyncTask<P, ?, ?>> void executeInParallel(T task) {
        executeInParallel(task, (P[]) null);
    }
    @SafeVarargs
    private static <P, T extends AsyncTask<P, ?, ?>> void executeInParallel(T task, P... params) {
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
    }
}
