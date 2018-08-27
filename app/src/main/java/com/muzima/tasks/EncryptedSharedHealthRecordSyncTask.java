package com.muzima.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.SmartCardRecord;
import com.muzima.controller.SmartCardController;

import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.UPLOAD_ERROR;

public class EncryptedSharedHealthRecordSyncTask {

    private static boolean syncInProgress;
    private static Context context;

    public static void uploadEncryptedSharedHealthRecords(Context context){
        EncryptedSharedHealthRecordSyncTask.context = context;
        if(syncInProgress){
             Toast.makeText(EncryptedSharedHealthRecordSyncTask.context,"Sync already in progress", Toast.LENGTH_LONG).show();
        } else {
            SyncSharedHealthRecordBackgroundTask syncSharedHealthRecordBackgroundTask = new SyncSharedHealthRecordBackgroundTask();
            syncSharedHealthRecordBackgroundTask.execute();
        }
    }
    private static class SyncSharedHealthRecordBackgroundTask extends AsyncTask<Void,Void,Integer[]>{

        @Override
        protected void onPreExecute() {

            Toast.makeText(EncryptedSharedHealthRecordSyncTask.context, "Uploading SHR Data", Toast.LENGTH_SHORT).show();
        }
        @Override
        protected void onPostExecute(Integer[] result){
            if(result[0] == SUCCESS) {
                Toast.makeText(EncryptedSharedHealthRecordSyncTask.context, "Uploaded " + result[1] +" SHR Data items", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(EncryptedSharedHealthRecordSyncTask.context, "Error Uploading SHR Data. Uploaded " +
                        result[1] + ", Failed " + result[2], Toast.LENGTH_SHORT).show();
            }
            syncInProgress = false;
        }

        @Override
        protected Integer[] doInBackground(Void... voids) {
            Integer[] result = {0,0,0};
            try {
                SmartCardController smartCardController = ((MuzimaApplication)context).getSmartCardController();

                List<SmartCardRecord> smartCardRecords = smartCardController.getSmartCardRecordWithNonUploadedData();
                for(SmartCardRecord smartCardRecord : smartCardRecords){
                    if (smartCardController.syncSmartCardRecord(smartCardRecord)) {
                        smartCardRecord.setEncryptedPayload(null);
                        try {
                            smartCardController.updateSmartCardRecord(smartCardRecord);
                        } catch (SmartCardController.SmartCardRecordSaveException e) {
                            Log.e(getClass().getSimpleName(), "Exception thrown while updating smartcard record.", e);
                        }
                        result[1]++;
                    } else {
                        result[0] = UPLOAD_ERROR;
                        result[2]++;
                    }
                }

                if(result[0] != UPLOAD_ERROR){
                    result[0] = SUCCESS;
                }
                return result;
            } catch (SmartCardController.SmartCardRecordFetchException e) {
                Log.e(getClass().getSimpleName(), "Exception thrown while uploading smartcard record.", e);
                result[0] = UPLOAD_ERROR;
            }
            return result;
        }
    }
}
