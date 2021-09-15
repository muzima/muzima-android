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

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Form;

import java.util.List;

public class DownloadFormsTask implements Runnable {
    private Context context;
    private List<Form> forms;
    private String[] formUuids;
    private FormsDownloadCallback callback;

    public DownloadFormsTask(Context context, List<Form> forms, FormsDownloadCallback callback) {
        this.context = context;
        this.callback = callback;
        this.forms = forms;
        this.formUuids = new String[forms.size()];
    }

    @Override
    public void run() {
        extractFormUuids();
        ((MuzimaApplication) context.getApplicationContext()).getMuzimaSyncService()
                .downloadFormTemplatesAndRelatedMetadata(formUuids, true);
        callback.formsDownloadFinished();
    }

    private void extractFormUuids() {
        for (int i = 0; i < formUuids.length; i++) {
            formUuids[i] = forms.get(i).getUuid();
        }
    }

    public interface FormsDownloadCallback {
        void formsDownloadFinished();
    }
}
