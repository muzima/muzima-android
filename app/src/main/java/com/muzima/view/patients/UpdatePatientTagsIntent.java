package com.muzima.view.patients;

import android.content.Context;
import android.content.Intent;

import com.muzima.service.DataSyncService;
import com.muzima.utils.Constants;

import java.util.List;

public class UpdatePatientTagsIntent extends Intent {
    private final Context context;

    public UpdatePatientTagsIntent(Context context, List<String> patientUuidsList){
        super(context, DataSyncService.class);
        this.context = context;
        putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.UPDATE_PATIENT_TAGS);
        putExtra(Constants.DataSyncServiceConstants.PATIENT_UUIDS, patientUuidsList.toArray(new String[patientUuidsList.size()]));

    }

    public void start() {
        context.startService(this);
    }
}