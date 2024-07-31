package com.muzima.controller;

import static com.muzima.api.model.APIName.DOWNLOAD_DERIVED_OBSERVATIONS;

import com.muzima.api.model.CohortMember;
import com.muzima.api.model.CohortMemberSummary;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.CohortMemberSummaryService;
import com.muzima.api.service.CohortService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;
import com.muzima.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CohortMemberSummaryController {

    private CohortMemberSummaryService cohortMemberSummaryService;

    private LastSyncTimeService lastSyncTimeService;
    private SntpService sntpService;

    private CohortService cohortService;

    public CohortMemberSummaryController(CohortMemberSummaryService cohortMemberSummaryService, LastSyncTimeService lastSyncTimeService, SntpService sntpService, CohortService cohortService) {
        this.cohortMemberSummaryService = cohortMemberSummaryService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
        this.cohortService = cohortService;
    }

    public void updateCohortMembersSummaries(List<CohortMemberSummary> summaryList) {
        try {
            for (CohortMemberSummary summary : summaryList) {
                List<CohortMember> cohortMembers = cohortService.getCohortMembershipByPatientUuid(summary.getPatientUuid());
                cohortMembers.get(0).setCohortMemberSummary(summary);
                this.cohortMemberSummaryService.updateCohortMemberSummary(cohortMembers.get(0));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public CohortMemberSummary getByPatientUuid(String patientUuid) {
        try {
            return this.cohortMemberSummaryService.getByPatientUuid(patientUuid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class CohortMemberSummaryDownloadException extends Throwable {
        CohortMemberSummaryDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    private String buildParamSignature(List<String> patientUuids) {
        return StringUtils.getCommaSeparatedStringFromList(patientUuids);
    }

    public List<CohortMemberSummary> downloadSummariesByPatientUuids(List<String> patientUuids) throws CohortMemberSummaryDownloadException {
        try {
            String paramSignature = buildParamSignature(patientUuids);
            Date lastSyncTime = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_DERIVED_OBSERVATIONS, paramSignature);
            List<CohortMemberSummary> summaryList = new ArrayList<>();
            summaryList.addAll(cohortMemberSummaryService.downloadSummariesByPatientUuidsAndSyncDate(patientUuids, lastSyncTime));

            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_DERIVED_OBSERVATIONS, sntpService.getTimePerDeviceTimeZone(), paramSignature);
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
            return summaryList;

        } catch (IOException e) {
            throw new CohortMemberSummaryDownloadException(e);
        }
    }
}
