package com.muzima.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.muzima.BroadcastListenerActivity;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;
import com.muzima.utils.AuthenticationManager;
import com.muzima.utils.Constants;

import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.CREDENTIALS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_FORMS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.*;

public class DataSyncService extends IntentService {

    private static final int MUZIMA_NOTIFICATION = 0;
    private static final String TAG = "DataSyncService";
    private final String notificationServiceRunning = "Muzima Sync Service Running";
    private final String notificationServiceFinished = "Muzima Sync Service Finished";
    private String notificationMsg;

    public DataSyncService() {
        super("DataSyncService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        updateNotificationMsg("Sync service started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int syncType = intent.getIntExtra(SYNC_TYPE, -1);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BroadcastListenerActivity.MESSAGE_SENT_ACTION);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, syncType);
        switch (syncType) {
            case SYNC_FORMS:
                updateNotificationMsg("Downloading Forms Metadata");
                int authenticationStatus = authenticate(intent);
                if (authenticationStatus != SyncStatusConstants.AUTHENTICATION_SUCCESS) {
                    broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, authenticationStatus);
                } else {
                    Integer[] result = downloadForms();
                    broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, result[0]);
                    if(result[0] == SUCCESS){
                        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT, result[1]);
                        updateNotificationMsg("Downloaded " + result[1] + " forms");
                    }
                }
                break;
            default:
                break;
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private int authenticate(Intent intent) {
        com.muzima.api.context.Context muzimaContext = ((MuzimaApplication) getApplication()).getMuzimaContext();
        return AuthenticationManager.authenticate(intent.getStringArrayExtra(CREDENTIALS), muzimaContext);
    }

    @Override
    public void onDestroy() {
        showNotification(notificationServiceFinished, notificationMsg);
        super.onDestroy();
    }

    private void showNotification(String title, String msg) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher_logo)
                        .setContentTitle(title)
                        .setContentText(msg);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(MUZIMA_NOTIFICATION, mBuilder.getNotification());
    }

    private Integer[] downloadForms() {
        Integer[] result = new Integer[2];

        FormController formController = ((MuzimaApplication) getApplication()).getFormController();

        try {
            List<Form> forms;
            forms = formController.downloadAllForms();
            Log.i(TAG, "Form download successful");
            formController.deleteAllForms();
            Log.i(TAG, "Old forms are deleted");
            formController.saveAllForms(forms);
            Log.i(TAG, "New forms are saved");

            result[0] = SyncStatusConstants.SUCCESS;
            result[1] = forms.size();

        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Exception when trying to download forms", e);
            result[0] = DOWNLOAD_ERROR;
            return result;
        } catch (FormController.FormSaveException e) {
            Log.e(TAG, "Exception when trying to save forms", e);
            result[0] = SAVE_ERROR;
            return result;
        } catch (FormController.FormDeleteException e) {
            Log.e(TAG, "Exception occurred while deleting existing forms", e);
            result[0] = DELETE_ERROR;
            return result;
        }
        return result;
    }

    private void updateNotificationMsg(String msg) {
        notificationMsg = msg;
        showNotification(notificationServiceRunning, notificationMsg);
    }
}
