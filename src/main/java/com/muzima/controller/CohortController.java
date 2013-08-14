package com.muzima.controller;

import com.muzima.api.model.Cohort;
import com.muzima.api.service.CohortService;
import com.muzima.search.api.util.StringUtil;

import java.io.IOException;
import java.util.List;

public class CohortController {
    private CohortService cohortService;

    public CohortController(CohortService cohortService) {
        this.cohortService = cohortService;
    }

    public List<Cohort> downloadAllCohorts() throws CohortDownloadException {
        try {
            return cohortService.downloadCohortsByName(StringUtil.EMPTY);
        } catch (IOException e) {
            throw new CohortDownloadException(e);
        }
    }

    public static class CohortDownloadException extends Throwable {
        public CohortDownloadException(Throwable throwable) {
            super(throwable);
        }
    }
}
