package com.muzima.controller;

import com.muzima.api.model.Cohort;
import com.muzima.api.model.Form;
import com.muzima.api.service.CohortService;
import com.muzima.search.api.util.StringUtil;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
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
}
