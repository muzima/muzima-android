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
