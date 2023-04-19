/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.tasks;

import static com.muzima.util.Constants.ServerSettings.COHORT_FILTER_DERIVED_CONCEPT_MAP;

import android.content.Context;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.DerivedObservation;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.Observation;
import com.muzima.controller.CohortController;
import com.muzima.controller.DerivedConceptController;
import com.muzima.controller.DerivedObservationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.model.CohortWithDerivedConceptFilter;
import com.muzima.utils.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LoadDownloadedCohortsTask implements Runnable {

    private Context context;
    private OnDownloadedCohortsLoadedCallback cohortsLoadedCallback;

    public LoadDownloadedCohortsTask(Context context, OnDownloadedCohortsLoadedCallback cohortsLoadedCallback) {
        this.context = context;
        this.cohortsLoadedCallback = cohortsLoadedCallback;
    }

    @Override
    public void run() {
        try {
            List<Cohort> downloadedCohorts = new ArrayList<>();
            List<CohortWithDerivedConceptFilter> cohortWithDerivedConceptFilters = new ArrayList<>();
            MuzimaSetting muzimaSetting = ((MuzimaApplication) context.getApplicationContext()).getMuzimaSettingController().getSettingByProperty(COHORT_FILTER_DERIVED_CONCEPT_MAP);
            DerivedConceptController derivedConceptController = ((MuzimaApplication) context.getApplicationContext()).getDerivedConceptController();
            DerivedObservationController derivedObservationController = ((MuzimaApplication) context.getApplicationContext()).getDerivedObservationController();
            for (Cohort cohort : ((MuzimaApplication) context.getApplicationContext()).getCohortController()
                    .getCohorts()) {
                if (((MuzimaApplication) context.getApplicationContext()).getCohortController().isDownloaded(cohort)) {
                    //String settingValue = muzimaSetting.getValueString();
                    String derivedConceptUuid = "";
                    String settingValue = "{ \"f0edf47a-196f-4fd0-9560-ebad8227ed27\" : \"ce620b7d-fe91-4849-ae0e-6d54bcf7a45e\", \"2beecc10-0f84-4f89-a499-fba7d333255b\" : \"ce620b7d-fe91-4849-ae0e-6d54bcf7a45e\"}";
                    JSONObject jsonObject = new JSONObject(settingValue);
                    if(jsonObject.has(cohort.getUuid())){
                        derivedConceptUuid = jsonObject.getString(cohort.getUuid());
                    }
                    if(!derivedConceptUuid.isEmpty()){
                        List<DerivedObservation> derivedObservations = derivedObservationController.getDerivedObservationByDerivedConceptUuid(derivedConceptUuid);
                        if(derivedObservations.size()>0) {
                            List<String> answerValues = new ArrayList<>();
                            for(DerivedObservation derivedObservation : derivedObservations) {
                                if(!answerValues.contains(derivedObservation.getValueAsString())) {
                                    cohortWithDerivedConceptFilters.add(new CohortWithDerivedConceptFilter(cohort, derivedConceptUuid, derivedObservation.getValueAsString()));
                                    answerValues.add(derivedObservation.getValueAsString());
                                }
                            }
                        } else {
                            cohortWithDerivedConceptFilters.add(new CohortWithDerivedConceptFilter(cohort,StringUtils.EMPTY, StringUtils.EMPTY));
                        }
                    }else{
                       cohortWithDerivedConceptFilters.add(new CohortWithDerivedConceptFilter(cohort,StringUtils.EMPTY,StringUtils.EMPTY));
                    }
                }
            }

            Collections.sort(cohortWithDerivedConceptFilters, new Comparator<CohortWithDerivedConceptFilter>() {
                @Override
                public int compare(CohortWithDerivedConceptFilter o1, CohortWithDerivedConceptFilter o2) {
                    return o1.getCohort().getName().compareTo(o2.getCohort().getName());
                }
            });
            cohortsLoadedCallback.onCohortsLoaded(cohortWithDerivedConceptFilters);
        } catch (CohortController.CohortFetchException ex) {
            ex.printStackTrace();
        } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (DerivedObservationController.DerivedObservationFetchException e) {
            e.printStackTrace();
        }
    }

    public interface OnDownloadedCohortsLoadedCallback {
        void onCohortsLoaded(List<CohortWithDerivedConceptFilter> cohortWithDerivedConceptFilters);
    }
}
