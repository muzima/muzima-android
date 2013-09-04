package com.muzima.controller;

import android.util.Log;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.CohortData;
import com.muzima.api.model.CohortMember;
import com.muzima.api.service.CohortService;
import com.muzima.search.api.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CohortController {
    private CohortService cohortService;

    public CohortController(CohortService cohortService) {
        this.cohortService = cohortService;
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
            return cohortService.downloadCohortsByName(StringUtil.EMPTY);
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
            return cohortService.downloadCohortData(uuid, false);
        } catch (IOException e) {
            throw new CohortDownloadException(e);
        }
    }

    public List<Cohort> downloadCohortsByPrefix(List<String> cohortPrefixes) throws CohortDownloadException {
        List<Cohort> filteredCohorts = new ArrayList<Cohort>();
        try {
            for (String cohortPrefix : cohortPrefixes) {
                List<Cohort> cohorts = cohortService.downloadCohortsByName(cohortPrefix);
                List<Cohort> filteredCohortsForPrefix = filterCohortsByPrefix(cohorts, cohortPrefix);
                addUniqueCohorts(filteredCohorts, filteredCohortsForPrefix);
//                filteredCohorts.addAll(filteredCohortsForPrefix);
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

    public List<Cohort> getSyncedCohorts() throws CohortFetchException {
        try {
            List<Cohort> cohorts = cohortService.getAllCohorts();
            List<Cohort> syncedCohorts = new ArrayList<Cohort>();
            for (Cohort cohort : cohorts) {
            //TODO: Have a has members method to make this more explicit
            if (cohortService.countCohortMembers(cohort.getUuid()) > 0) {
                    syncedCohorts.add(cohort);
                }
            }
            return syncedCohorts;
        } catch (IOException e) {
            throw new CohortFetchException(e);
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

    public void addCohortMembers(List<CohortMember> cohortMembers) throws  CohortReplaceException {
        try {
            cohortService.saveCohortMembers(cohortMembers);
        } catch (IOException e) {
            throw new CohortReplaceException(e);
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
