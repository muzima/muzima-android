package com.muzima.controller;

import com.muzima.api.model.Cohort;
import com.muzima.api.model.CohortData;
import com.muzima.api.model.CohortDefinition;
import com.muzima.api.model.CohortMember;
import com.muzima.api.model.Form;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.service.CohortService;
import com.muzima.search.api.util.StringUtil;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CohortController {
    private CohortService cohortService;

    public CohortController(CohortService cohortService) {
        this.cohortService = cohortService;
    }

    public List<Cohort> getAllCohorts() throws CohortFetchException{
        try {
            return cohortService.getAllCohorts();
        } catch (IOException e) {
            throw new CohortFetchException(e);
        } catch (ParseException e) {
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

    public void saveAllCohorts(List<Cohort> cohorts) throws CohortSaveException {
        try {
            for (Cohort cohort : cohorts) {
                cohortService.saveCohort(cohort);
            }
        } catch (IOException e) {
            throw new CohortSaveException(e);
        }

    }

    public void deleteAllCohorts() throws CohortDeleteException {
        try {
            List<Cohort> allCohorts = cohortService.getAllCohorts();
            for (Cohort cohort : allCohorts) {
                cohortService.deleteCohort(cohort);
            }
        } catch (IOException e) {
            throw new CohortDeleteException(e);
        } catch (ParseException e) {
            throw new CohortDeleteException(e);
        }
    }

    public void replaceCohortMembers(String cohortUuid, List<CohortMember> cohortMembers) throws CohortReplaceException {
        try {
            cohortService.deleteCohortMembers(cohortUuid);
            cohortService.saveCohortMembers(cohortMembers);
        } catch (IOException e) {
            throw new CohortReplaceException(e);
        }
    }

    public List<Cohort> getSyncedCohort() throws CohortFetchException {
        try {
            //TODO this is very inefficient, should have a download flag in cohorts
            List<Cohort> cohorts = cohortService.getAllCohorts();
            List<Cohort> syncedCohorts = new ArrayList<Cohort>();
            for (Cohort cohort : cohorts) {
                if(!cohortService.getCohortMembers(cohort.getUuid()).isEmpty()){
                    syncedCohorts.add(cohort);
                }
            }
            return syncedCohorts;
        } catch (IOException e) {
            throw new CohortFetchException(e);
        } catch (ParseException e) {
            throw new CohortFetchException(e);
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
