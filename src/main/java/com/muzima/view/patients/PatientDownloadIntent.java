package com.muzima.view.patients;

import android.app.Activity;
import com.muzima.utils.Constants;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants;

public class PatientDownloadIntent extends SyncIntent {
    public PatientDownloadIntent(Activity activity, String[] patientUUIDs) {
        super(activity);
        putExtra(DataSyncServiceConstants.PATIENT_UUID_FOR_DOWNLOAD, patientUUIDs);
        putExtra(DataSyncServiceConstants.SYNC_TYPE, DataSyncServiceConstants.DOWNLOAD_PATIENT_ONLY);
    }
}
