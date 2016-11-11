package com.muzima.utils;

import android.os.AsyncTask;
import android.os.Build;

/**
 * Created by savai on 8/25/16.
 */
public class BackgroundTaskHelper {
    public static <P, T extends AsyncTask<P, ?, ?>> void executeInParallel(T task) {
        executeInParallel(task, (P[]) null);
    }
    public static <P, T extends AsyncTask<P, ?, ?>> void executeInParallel(T task, P... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            task.execute(params);
        }
    }
}
