package com.muzima.utils;

import android.os.AsyncTask;
import android.os.Build;

public class BackgroundTaskHelper {
    public static <P, T extends AsyncTask<P, ?, ?>> void executeInParallel(T task) {
        executeInParallel(task, (P[]) null);
    }
    public static <P, T extends AsyncTask<P, ?, ?>> void executeInParallel(T task, P... params) {
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
    }
}
