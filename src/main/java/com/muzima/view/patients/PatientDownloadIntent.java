package com.muzima.view.patients;

import android.app.Activity;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants.*;

public class PatientDownloadIntent extends SyncIntent {
    public PatientDownloadIntent(Activity activity, String[] patientUUIDs) {
        super(activity);
        putExtra(PATIENT_UUID_FOR_DOWNLOAD, patientUUIDs);
        putExtra(SYNC_TYPE, DOWNLOAD_PATIENT_ONLY);
    }
}
