/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.reports;

import android.content.Context;
import androidx.fragment.app.FragmentManager;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.controller.PatientReportController;
import com.muzima.utils.LanguageUtil;
import com.muzima.view.reports.AllPatientReportListFragment;
import com.muzima.view.reports.DownloadedPatientReportListFragment;

/**
 * Responsible to hold all the report fragments as multiple pages/tabs.
 */
public class PatientReportPagerAdapter extends MuzimaPagerAdapter {

    public static final int TAB_AVAILABLE= 0;
    public static final int TAB_All= 1;
    private final String patientUuid;

    public PatientReportPagerAdapter(Context context, FragmentManager supportFragmentManager, String patientUuid) {
        super(context, supportFragmentManager);
        this.patientUuid = patientUuid;
    }

    public void initPagerViews(){
        pagers = new PagerView[2];
        PatientReportController patientReportController = ((MuzimaApplication) context.getApplicationContext()).getPatientReportController();

        DownloadedPatientReportListFragment downloadedPatientReportListFragment = DownloadedPatientReportListFragment.newInstance(patientReportController, patientUuid);
        AllPatientReportListFragment allPatientReportListFragment = AllPatientReportListFragment.newInstance(patientReportController, patientUuid);

        allPatientReportListFragment.setPatientReportsDownloadListener(downloadedPatientReportListFragment);

        LanguageUtil languageUtil = new LanguageUtil();
        Context localizedContext = languageUtil.getLocalizedContext(context);

        pagers[TAB_AVAILABLE] = new PagerView(localizedContext.getResources().getString(R.string.downloaded_reports), downloadedPatientReportListFragment);
        pagers[TAB_All] = new PagerView(localizedContext.getResources().getString(R.string.get_more), allPatientReportListFragment);
    }

    public void onPatientReportsDownloadStart() {
        ((AllPatientReportListFragment)pagers[TAB_All].fragment).onPatientReportsDownloadStart();
    }

    public void onPatientReportsDownloadFinish() {
        ((AllPatientReportListFragment)pagers[TAB_All].fragment).onPatientReportsDownloadFinish();
    }

    public void onSelectedReportDownloadFinish() {
        ((AllPatientReportListFragment)pagers[TAB_All].fragment).onSelectedReportDownloadFinish();
    }

    public void reinitializeAllPatientReportsTab() {
        pagers[TAB_All].fragment.unselectAllItems();
        ((AllPatientReportListFragment)pagers[TAB_All].fragment).endActionMode();
    }
}
