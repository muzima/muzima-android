package com.muzima.view.reports;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_REPORT_DATASETS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;

import android.content.Context;

import com.muzima.view.SyncIntent;

public class SyncReportDatasets extends SyncIntent {
    public SyncReportDatasets(Context context) {
        super(context);
        putExtra(SYNC_TYPE, SYNC_REPORT_DATASETS);
    }
}
