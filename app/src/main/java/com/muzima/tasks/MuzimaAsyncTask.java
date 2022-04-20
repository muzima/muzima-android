/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.tasks;

import java.util.concurrent.ExecutorService;

public abstract class MuzimaAsyncTask<INPUT, PROGRESS, OUTPUT> {
    private boolean cancelled = false;

    public MuzimaAsyncTask() {}

    /**
     * Starts all
     * @param input Data you want to process in the background
     */
    public void execute(final INPUT... input) {
        onPreExecute();
        ExecutorService executorService = AsyncWorker.getInstance().getExecutorService();
        executorService.execute(() -> {
            try {
                final OUTPUT output = doInBackground(input);
                if(!isCancelled())
                    AsyncWorker.getInstance().getHandler().post(() -> onPostExecute(output));
            } catch (final Exception e) {
                e.printStackTrace();

                AsyncWorker.getInstance().getHandler().post(() -> onBackgroundError(e));
            }
        });
    }

    public void execute() {
        execute(null);
    }

    /**
     * Call to publish progress from background
     * @param progress  Progress made
     */
    protected void publishProgress(final PROGRESS progress) {
        AsyncWorker.getInstance().getHandler().post(() -> {
            if (onProgressListener != null) {
                onProgressListener.onProgress(progress);
            }
        });
    }

    /**
     * Call to cancel background work
     */
    public void cancel() {
        cancelled = true;
    }

    /**
     *
     * @return Returns true if the background work should be cancelled
     */
    protected boolean isCancelled() {
        return cancelled;
    }

    /**
     * Call this method after cancelling background work
     */
    protected void onCancelled() {
        AsyncWorker.getInstance().getHandler().post(() -> {
            if (onCancelledListener != null) {
                onCancelledListener.onCancelled();
            }
        });
    }

    /**
     * Work which you want to be done on UI thread before {@link #doInBackground(Object...)}
     */
    protected abstract void onPreExecute();

    /**
     * Work on background
     *
     * @param input Input data
     * @return Output data
     * @throws Exception Any uncaught exception which occurred while working in background.
     *  If any occurs, {@link #onBackgroundError(Exception)} will be executed (on the UI thread)
     */
    protected abstract OUTPUT doInBackground(INPUT... input) throws Exception;

    /**
     * Work which you want to be done on UI thread after {@link #doInBackground(Object...)}
     * @param output Output data from {@link #doInBackground(Object...)}
     */
    protected abstract void onPostExecute(OUTPUT output);

    /**
     * Triggered on UI thread if any uncaught exception occurred while working in background
     * @param e Exception
     * @see #doInBackground(Object...)
     */
    protected abstract void onBackgroundError(Exception e);

    private OnProgressListener<PROGRESS> onProgressListener;

    public interface OnProgressListener<PROGRESS> {
        void onProgress(PROGRESS progress);
    }

    public void setOnProgressListener(OnProgressListener<PROGRESS> onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

    private OnCancelledListener onCancelledListener;

    public interface OnCancelledListener {
        void onCancelled();
    }

    public void setOnCancelledListener(OnCancelledListener onCancelledListener) {
        this.onCancelledListener = onCancelledListener;
    }
}
