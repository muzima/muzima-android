package com.muzima.view.reports;

import android.content.Context;

import com.muzima.view.SyncIntent;
import com.muzima.utils.Constants;

public class SyncReportDatasets extends SyncIntent {
    public SyncReportDatasets(Context context) {
        super(context);
        putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_REPORT_DATASETS);
    }
}