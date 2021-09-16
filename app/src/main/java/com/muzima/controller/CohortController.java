/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.CohortData;
import com.muzima.api.model.CohortMember;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.Provider;
import com.muzima.api.model.User;
import com.muzima.api.service.CohortService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;
import com.muzima.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_COHORTS;
import static com.muzima.api.model.APIName.DOWNLOAD_COHORTS_DATA;
import static com.muzima.api.model.APIName.DOWNLOAD_REMOVED_COHORTS_DATA;

public class CohortController {
    private static final String TAG = "CohortController";
    private final CohortService cohortService;
    private final LastSyncTimeService lastSyncTimeService;
    private final SntpService sntpService;
    private final MuzimaApplication muzimaApplication;

    public CohortController(CohortService cohortService, LastSyncTimeService lastSyncTimeService, SntpService sntpService, MuzimaApplication muzimaApplication) {
        this.cohortService = cohortService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
        this.muzimaApplication = muzimaApplication;
    }

    public String getDefaultLocation() {
        Context context = muzimaApplication.getApplicationContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String setDefaultLocation = preferences.getString("defaultEncounterLocation", null);
        if (setDefaultLocation == null) {
            setDefaultLocation = null;
        }
        return setDefaultLocation;
    }

    public Provider getLoggedInProvider() {
        Provider loggedInProvider = new Provider();
        try {
            User authenticatedUser = muzimaApplication.getAuthenticatedUser();
            if (authenticatedUser != null) {
                loggedInProvider = muzimaApplication.getProviderController().getLoggedInProvider(
                        muzimaApplication.getAuthenticatedUser().getSystemId());
            } else {
                loggedInProvider = null;
            }
        } catch (ProviderController.ProviderLoadException e) {
            loggedInProvider = null;
            Log.e(getClass().getSimpleName(), "Exception while fetching logged in provider " + e);
        }
        return loggedInProvider;
    }


    public List<Cohort> getAllCohorts() throws CohortFetchException {
        try {
            return cohortService.getAllCohorts();
        } catch (IOException e) {
            throw new CohortFetchException(e);
        }
    }

    public int countAllCohorts() throws CohortFetchException {
        try {
            return cohortService.countAllCohorts();
        } catch (IOException e) {
            throw new CohortFetchException(e);
        }
    }

    public List<Cohort> downloadAllCohorts(String defaultLocation) throws CohortDownloadException {
        try {
            Date lastSyncTimeForCohorts = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS);
            Provider loggedInProvider = getLoggedInProvider();
            List<Cohort> allCohorts = cohortService.downloadCohortsByNameAndSyncDate(StringUtils.EMPTY, lastSyncTimeForCohorts, defaultLocation, loggedInProvider);
            LastSyncTime lastSyncTime = new LastSyncTime(DOWNLOAD_COHORTS, sntpService.getTimePerDeviceTimeZone());
            lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            return allCohorts;
        } catch (IOException e) {
            throw new CohortDownloadException(e);
        }
    }

    public List<CohortData> downloadCohortData(String[] cohortUuids, String defaulLocation) throws CohortDownloadException {
        ArrayList<CohortData> allCohortData = new ArrayList<>();
        for (String cohortUuid : cohortUuids) {
            allCohortData.add(downloadCohortDataByUuid(cohortUuid, defaulLocation));
        }
        return allCohortData;
    }

    public List<CohortData> downloadRemovedCohortData(String[] cohortUuids) throws CohortDownloadException {
        ArrayList<CohortData> allCohortData = new ArrayList();
        for (String cohortUuid : cohortUuids) {
            allCohortData.add(downloadRemovedCohortDataByUuid(cohortUuid));
        }
        return allCohortData;
    }

    public CohortData downloadCohortDataByUuid(String uuid, String defaultLocation) throws CohortDownloadException {
        try {
            Date lastSyncDate = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS_DATA, uuid);
            Provider loggedInProvider = getLoggedInProvider();
            CohortData cohortData = cohortService.downloadCohortDataAndSyncDate(uuid, false, lastSyncDate, defaultLocation, loggedInProvider);
            LastSyncTime lastSyncTime = new LastSyncTime(DOWNLOAD_COHORTS_DATA, sntpService.getTimePerDeviceTimeZone(), uuid);
            lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            return cohortData;
        } catch (IOException e) {
            throw new CohortDownloadException(e);
        }
    }

    public CohortData downloadRemovedCohortDataByUuid(String uuid) throws CohortDownloadException {
        try {
            String defaultLocation = getDefaultLocation();
            Provider loggedInProvider = getLoggedInProvider();
            Date lastSyncDate = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_REMOVED_COHORTS_DATA, uuid);
            CohortData cohortData = cohortService.downloadRemovedStaticCohortMemberByCohortUuidAndSyncDate(uuid, lastSyncDate, defaultLocation, loggedInProvider);
            LastSyncTime lastSyncTime = new LastSyncTime(DOWNLOAD_REMOVED_COHORTS_DATA, sntpService.getTimePerDeviceTimeZone(), uuid);
            lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            return cohortData;
        } catch (IOException e) {
            throw new CohortDownloadException(e);
        }
    }

    public List<Cohort> downloadCohortsByPrefix(List<String> cohortPrefixes, String defaultLocation) throws CohortDownloadException {
        Log.e(TAG, "downloadCohortsByPrefix: " + cohortPrefixes);
        Provider loggedInProvider = getLoggedInProvider();
        List<Cohort> filteredCohorts = new ArrayList<>();
        try {
            Date lastSyncDateOfCohort;
            LastSyncTime lastSyncTime;
            for (String cohortPrefix : cohortPrefixes) {
                lastSyncDateOfCohort = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS, cohortPrefix);
                List<Cohort> cohorts = cohortService.downloadCohortsByNameAndSyncDate(cohortPrefix, lastSyncDateOfCohort, defaultLocation, loggedInProvider);
                Log.e(TAG, "downloadCohortsByPrefix: downloadCohortsByNameAndSyncDate cohorts " + cohorts.size());
                List<Cohort> filteredCohortsForPrefix = filterCohortsByPrefix(cohorts, cohortPrefix);
                addUniqueCohorts(filteredCohorts, filteredCohortsForPrefix);
                lastSyncTime = new LastSyncTime(DOWNLOAD_COHORTS, sntpService.getTimePerDeviceTimeZone(), cohortPrefix);
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
        ArrayList<Cohort> filteredCohortList = new ArrayList<>();
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

    public void saveOrUpdateCohorts(List<Cohort> cohorts) throws CohortSaveException {
        try {
            for (Cohort cohort : cohorts) {
                if (StringUtils.isEmpty(cohort.getUuid())) {
                    cohortService.saveCohort(cohort);
                } else {
                    Cohort localCohort = cohortService.getCohortByUuid(cohort.getUuid());
                    if (localCohort != null) {
                        cohort.setSyncStatus(localCohort.getSyncStatus());
                    }
                    cohortService.updateCohort(cohort);
                }
            }
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
            List<Cohort> syncedCohorts = new ArrayList<>();
            for (Cohort cohort : cohorts) {
                if (isDownloaded(cohort)) {
                    syncedCohorts.add(cohort);
                }
            }
            return syncedCohorts;
        } catch (IOException e) {
            throw new CohortFetchException(e);
        }
    }

    public List<Cohort> getUnSyncedCohorts() throws CohortFetchException {
        try {
            List<Cohort> cohorts = cohortService.getAllCohorts();
            List<Cohort> unSyncedCohorts = new ArrayList<>();
            for (Cohort cohort : cohorts) {
                if (!isDownloaded(cohort)) {
                    unSyncedCohorts.add(cohort);
                }
            }
            return unSyncedCohorts;
        } catch (IOException e) {
            throw new CohortFetchException(e);
        }
    }

    public boolean isDownloaded(Cohort cohort) {
        try {
            Cohort localCohort = cohortService.getCohortByUuid(cohort.getUuid());
            if (localCohort != null) {
                //Returning the checking of downloaded cohorts by both sync status and cohort member count.
                //This is to enable successful upgrades from lower versions to 2.5.0(Lower version did not have sync status)
                //TODO: should remove the check by cohort member count once implementation have upgraded to version 2.5.0
                return localCohort.getSyncStatus() == 1 || cohortService.countCohortMembers(cohort.getUuid()) > 0;
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isUpdateAvailable() throws CohortFetchException {
        for (Cohort cohort : getSyncedCohorts()) {
            if (cohort.isUpdateAvailable()) {
                return true;
            }
        }
        return false;
    }

    public List<Cohort> getCohortsWithPendingUpdates() throws CohortFetchException {
        List<Cohort> cohortList = new ArrayList<>();
        for (Cohort cohort : getSyncedCohorts()) {
            if (cohort.isUpdateAvailable()) {
                cohortList.add(cohort);
            }
        }
        return cohortList;
    }


    public void markAsUpToDate(String[] cohortUuids) throws CohortUpdateException {
        ArrayList<CohortData> allCohortData = new ArrayList<>();

        try {
            for (String cohortUuid : cohortUuids) {
                Cohort cohort = cohortService.getCohortByUuid(cohortUuid);
                if (cohort != null) {
                    cohort.setUpdateAvailable(false);
                    cohortService.updateCohort(cohort);
                }
            }
        } catch (IOException e) {
            throw new CohortUpdateException(e);
        }
    }

    public void setSyncStatus(String[] cohortUuids) throws CohortUpdateException {
        try {
            for (String cohortUuid : cohortUuids) {
                Cohort cohort = cohortService.getCohortByUuid(cohortUuid);
                if (cohort != null) {
                    cohort.setSyncStatus(1);
                    cohortService.updateCohort(cohort);
                }
            }
        } catch (IOException e) {
            throw new CohortUpdateException(e);
        }
    }

    public int countSyncedCohorts() throws CohortFetchException {
        return getSyncedCohorts().size();
    }

    public void deleteAllCohortMembers(String cohortUuid) throws CohortReplaceException {
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

    public int countCohortMembers(Cohort cohort) throws CohortFetchException {
        try {
            return cohortService.countCohortMembers(cohort);
        } catch (IOException e) {
            throw new CohortFetchException(e);
        }
    }

    public List<Cohort> downloadCohortByName(String name) throws CohortDownloadException {
        String defaultLocation = getDefaultLocation();
        Provider loggedInProvider = getLoggedInProvider();
        try {
            return cohortService.downloadCohortsByName(name, defaultLocation, loggedInProvider);
        } catch (IOException e) {
            throw new CohortDownloadException(e);
        }
    }

    public List<Cohort> downloadCohortsByUuidList(String[] uuidList) throws CohortDownloadException {
        String defaultLocation = getDefaultLocation();
        Provider loggedInProvider = getLoggedInProvider();
        try {
            List<Cohort> cohortList = new ArrayList<>();
            for (String uuid : uuidList) {
                cohortList.add(cohortService.downloadCohortByUuid(uuid, defaultLocation, loggedInProvider));
            }
            return cohortList;
        } catch (IOException e) {
            throw new CohortDownloadException(e);
        }
    }

    public List<CohortMember> getCohortMembershipByPatientUuid(String patientUuid) throws CohortFetchException {
        try {
            List<CohortMember> cohortMembers = cohortService.getCohortMembershipByPatientUuid(patientUuid);
            return cohortMembers;
        } catch (IOException e) {
            throw new CohortFetchException(e);
        }
    }

    public void deleteAllCohortMembers(List<Cohort> allCohorts) throws CohortReplaceException {
        for (Cohort cohort : allCohorts) {
            deleteAllCohortMembers(cohort.getUuid());
        }
    }

    public void deleteCohortMembers(List<CohortMember> cohortMembers) throws CohortDeleteException {
        try {
            cohortService.deleteCohortMembers(cohortMembers);
        } catch (IOException e) {
            throw new CohortDeleteException(e);
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

    public static class CohortUpdateException extends Throwable {
        public CohortUpdateException(Throwable throwable) {
            super(throwable);
        }
    }
}
