package com.muzima.tasks;

import android.util.Log;
import com.muzima.api.context.Context;
import com.muzima.api.context.ContextFactory;
import com.muzima.api.model.Form;
import com.muzima.api.service.FormService;
import com.muzima.search.api.util.StringUtil;

import java.io.File;
import java.util.List;

public class DownloadFormTask extends DownloadTask<String, Void, Integer[]> {
    private static final String TAG = "DownloadFormTask";

    public static final int ERROR = 0;
    public static final int SUCCESS = 1;

    private Context muzimaContext;

    public DownloadFormTask(Context context){
        muzimaContext = context;
    }

    @Override
    protected Integer[] doInBackground(String... values) {
        Integer[] result = new Integer[2];

        String username = values[0];
        String password = values[1];
        String server = values[2];

        try {
            muzimaContext.openSession();
            if (!muzimaContext.isAuthenticated()) {
                muzimaContext.authenticate(username, password, server);
            }

            FormService formService = muzimaContext.getFormService();

            List<Form> forms = formService.downloadFormsByName(StringUtil.EMPTY);
            deleteLuceneCache();
            if (!forms.isEmpty()) {
                for (Form form : forms) {
                    formService.saveForm(form);
                }
            }
            result[1] = forms.size();
        } catch (Exception e) {
            Log.e(TAG, "Exception when trying to load forms", e);
            result[0] = ERROR;
        } finally {
            if (muzimaContext != null)
                muzimaContext.closeSession();
        }

        result[0] = SUCCESS;
        return result;
    }

    private void deleteLuceneCache() {
        File luceneDirectory = new File(System.getProperty("java.io.tmpdir") + "/lucene");
        int numberOfFiles = luceneDirectory.listFiles().length;
        Log.d(TAG, "Number of files in lucene directory: " + numberOfFiles);
        int fileCounter = 0;
        for (String filename : luceneDirectory.list()) {
            File file = new File(luceneDirectory, filename);
            if(file.delete())   fileCounter++;
        }
        Log.d(TAG, "Number of deleted files: " + fileCounter);
    }
}
