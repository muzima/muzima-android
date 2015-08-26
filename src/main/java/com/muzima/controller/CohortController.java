/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.controller;

import com.muzima.api.model.Cohort;
import com.muzima.api.model.CohortData;
import com.muzima.api.model.CohortMember;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.CohortService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.search.api.util.StringUtil;
import com.muzima.service.SntpService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_COHORTS;
import static com.muzima.api.model.APIName.DOWNLOAD_COHORTS_DATA;

public class CohortController {
    private static final String TAG = "CohortController";
    private CohortService cohortService;
    private LastSyncTimeService lastSyncTimeService;
    private SntpService sntpService;

    public CohortController(CohortService cohortService, LastSyncTimeService lastSyncTimeService, SntpService sntpService) {
        this.cohortService = cohortService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
    }

    public List<Cohort> getAllCohorts() throws CohortFetchException {
        try {
            return cohortService.getAllCohorts();
        } catch (IOException e) {
            throw new CohortFetchException(e);
        }
    }

    public int getTotalCohortsCount() throws CohortFetchException {
        try {
            return cohortService.countAllCohorts();
        } catch (IOException e) {
            throw new CohortFetchException(e);
        }
    }

    public List<Cohort> downloadAllCohorts() throws CohortDownloadException {
        try {
            Date lastSyncTimeForCohorts = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS);
            List<Cohort> allCohorts = cohortService.downloadCohortsByNameAndSyncDate(StringUtil.EMPTY, lastSyncTimeForCohorts);

            LastSyncTime lastSyncTime = new LastSyncTime(DOWNLOAD_COHORTS, sntpService.getLocalTime());
            lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            return allCohorts;
        } catch (IOException e) {
            throw new CohortDownloadException(e);
        }
    }

    public List<CohortData> downloadCohortData(String[] cohortUuids) throws CohortDownloadException {
        ArrayList<CohortData> allCohortData = new ArrayList<CohortData>();
        for (String cohortUuid : cohortUuids) {
            allCohortData.add(downloadCohortDataByUuid(cohortUuid));
        }
        return allCohortData;
    }

    public CohortData downloadCohortDataByUuid(String uuid) throws CohortDownloadException {
        try {
            Date lastSyncDate = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS_DATA, uuid);
            CohortData cohortData = cohortService.downloadCohortDataAndSyncDate(uuid, false, lastSyncDate);
            LastSyncTime lastSyncTime = new LastSyncTime(DOWNLOAD_COHORTS_DATA, sntpService.getLocalTime(), uuid);
            lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            return cohortData;
        } catch (IOException e) {
            throw new CohortDownloadException(e);
        }
    }

    public List<Cohort> downloadCohortsByPrefix(List<String> cohortPrefixes) throws CohortDownloadException {
        List<Cohort> filteredCohorts = new ArrayList<Cohort>();
        try {
            Date lastSyncDateOfCohort;
            LastSyncTime lastSyncTime;
            for (String cohortPrefix : cohortPrefixes) {
                lastSyncDateOfCohort = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS, cohortPrefix);
                List<Cohort> cohorts = cohortService.downloadCohortsByNameAndSyncDate(cohortPrefix, lastSyncDateOfCohort);
                List<Cohort> filteredCohortsForPrefix = filterCohortsByPrefix(cohorts, cohortPrefix);
                addUniqueCohorts(filteredCohorts, filteredCohortsForPrefix);
                lastSyncTime = new LastSyncTime(DOWNLOAD_COHORTS, sntpService.getLocalTime(), cohortPrefix);
                lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            }
        } catch (IOException e) {
            throw new CohortDownloadException(e);
        }
        return filteredCohorts;
    }

    private void addUniqueCohorts(List<Cohort> filteredCohorts, List<Cohort> filteredCohortsForPrefix) {
        for (Cohort fileteredCohortForPrefix : filteredCohortsForPrefix) {
            boolean found = false;
            for (Cohort filteredCohort : filteredCohorts) {
                if (fileteredCohortForPrefix.getUuid().equals(filteredCohort.getUuid())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                filteredCohorts.add(fileteredCohortForPrefix);
            }
        }
    }

    private List<Cohort> filterCohortsByPrefix(List<Cohort> cohorts, String cohortPrefix) {
        ArrayList<Cohort> filteredCohortList = new ArrayList<Cohort>();
        for (Cohort cohort : cohorts) {
            String lowerCaseCohortName = cohort.getName().toLowerCase();
            String lowerCasePrefix = cohortPrefix.toLowerCase();
            if (lowerCaseCohortName.startsWith(lowerCasePrefix)) {
                filteredCohortList.add(cohort);
            }
        }
        return filteredCohortList;
    }

    public void saveAllCohorts(List<Cohort> cohorts) throws CohortSaveException {
        try {
            cohortService.saveCohorts(cohorts);
        } catch (IOException e) {
            throw new CohortSaveException(e);
        }

    }

    public void deleteAllCohorts() throws CohortDeleteException {
        try {
            cohortService.deleteCohorts(cohortService.getAllCohorts());
        } catch (IOException e) {
            throw new CohortDeleteException(e);
        }
    }

    public void deleteCohorts(List<Cohort> cohorts) throws CohortDeleteException {
        try {
            cohortService.deleteCohorts(cohorts);
        } catch (IOException e) {
            throw new CohortDeleteException(e);
        }
    }

    public List<Cohort> getSyncedCohorts() throws CohortFetchException {
        try {

            List<Cohort> cohorts = cohortService.getAllCohorts();
            List<Cohort> syncedCohorts = new ArrayList<Cohort>();
            for (Cohort cohort : cohorts) {
                //TODO: Have a has members method to make this more explicit
                if (isDownloaded(cohort)) {
                    syncedCohorts.add(cohort);
                }
            }
            return syncedCohorts;
        } catch (IOException e) {
            throw new CohortFetchException(e);
        }
    }

    public boolean isDownloaded(Cohort cohort) {
        try {
            return cohortService.countCohortMembers(cohort.getUuid()) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    public int getSyncedCohortsCount() throws CohortFetchException {
        return getSyncedCohorts().size();
    }

    public void deleteCohortMembers(String cohortUuid) throws CohortReplaceException {
        try {
            cohortService.deleteCohortMembers(cohortUuid);
        } catch (IOException e) {
            throw new CohortReplaceException(e);
        }

    }

    public void addCohortMembers(List<CohortMember> cohortMembers) throws CohortReplaceException {
        try {
            cohortService.saveCohortMembers(cohortMembers);
        } catch (IOException e) {
            throw new CohortReplaceException(e);
        }

    }

    public List<Cohort> downloadCohortByName(String name) throws CohortDownloadException {
        try {
            return cohortService.downloadCohortsByName(name);
        } catch (IOException e) {
            throw new CohortDownloadException(e);
        }
    }

    public void deleteCohortMembers(List<Cohort> allCohorts) throws CohortReplaceException {
        for (Cohort cohort : allCohorts) {
            deleteCohortMembers(cohort.getUuid());
        }
    }

    public static class CohortDownloadException extends Throwable {
        public CohortDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class CohortFetchException extends Throwable {
        public CohortFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class CohortSaveException extends Throwable {
        public CohortSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class CohortDeleteException extends Throwable {
        public CohortDeleteException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class CohortReplaceException extends Throwable {
        public CohortReplaceException(Throwable throwable) {
            super(throwable);
        }
    }
}
