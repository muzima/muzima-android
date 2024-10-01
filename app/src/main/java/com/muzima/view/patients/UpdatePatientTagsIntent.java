/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.patients;

import android.content.Context;
import android.content.Intent;

import com.muzima.service.DataSyncService;
import com.muzima.utils.Constants;

import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.PATIENT_UUIDS;

public class UpdatePatientTagsIntent extends Intent {
    private final Context context;

    public UpdatePatientTagsIntent(Context context, List<String> patientUuidsList){
        super(context, DataSyncService.class);
        this.context = context;
        putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.UPDATE_PATIENT_TAGS);
        putExtra(PATIENT_UUIDS, patientUuidsList.toArray(new String[patientUuidsList.size()]));

    }

    public void start() {
        context.startService(this);
    }
}