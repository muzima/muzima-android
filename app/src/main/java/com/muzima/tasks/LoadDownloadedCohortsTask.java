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
import static com.muzima.utils.ConceptUtils.getDerivedConceptNameFromConceptNamesByLocale;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.DerivedConcept;
import com.muzima.api.model.DerivedObservation;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.controller.CohortController;
import com.muzima.controller.DerivedConceptController;
import com.muzima.controller.DerivedObservationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.model.CohortWithDerivedConceptFilter;
import com.muzima.utils.StringUtils;

import org.json.JSONArray;
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
            List<CohortWithDerivedConceptFilter> cohortWithDerivedConceptFilters = new ArrayList<>();
            MuzimaSetting muzimaSetting = ((MuzimaApplication) context.getApplicationContext()).getMuzimaSettingController().getSettingByProperty(COHORT_FILTER_DERIVED_CONCEPT_MAP);
            DerivedObservationController derivedObservationController = ((MuzimaApplication) context.getApplicationContext()).getDerivedObservationController();
            DerivedConceptController derivedConceptController = ((MuzimaApplication) context.getApplicationContext()).getDerivedConceptController();
            for (Cohort cohort : ((MuzimaApplication) context.getApplicationContext()).getCohortController()
                    .getCohorts()) {
                if (((MuzimaApplication) context.getApplicationContext()).getCohortController().isDownloaded(cohort)) {
                    String settingValue = muzimaSetting.getValueString();
                    if(settingValue != null) {
                        String derivedConceptUuid = "";
                        Object derivedConceptObject = null;
                        JSONObject jsonObject = new JSONObject(settingValue);
                        if (jsonObject.has(cohort.getUuid())) {
                            derivedConceptObject = jsonObject.get(cohort.getUuid());
                            if (derivedConceptObject != null && derivedConceptObject instanceof JSONArray) {
                                JSONArray jsonArray = (JSONArray) derivedConceptObject;
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    derivedConceptUuid = jsonArray.get(i).toString();
                                    if (!derivedConceptUuid.isEmpty()) {
                                        List<DerivedObservation> derivedObservations = derivedObservationController.getDerivedObservationByDerivedConceptUuid(derivedConceptUuid);
                                        if (derivedObservations.size() > 0) {
                                            List<String> answerValues = new ArrayList<>();
                                            for (DerivedObservation derivedObservation : derivedObservations) {
                                                if (!answerValues.contains(derivedObservation.getValueAsString())) {
                                                    cohortWithDerivedConceptFilters.add(new CohortWithDerivedConceptFilter(cohort, derivedConceptUuid, derivedObservation.getValueAsString()));
                                                    answerValues.add(derivedObservation.getValueAsString());
                                                }
                                            }
                                        } else {
                                            DerivedConcept derivedConcept = derivedConceptController.getDerivedConceptByUuid(derivedConceptUuid);
                                            if(derivedConcept != null) {
                                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                                                String applicationLanguage = preferences.getString(context.getResources().getString(R.string.preference_app_language), context.getResources().getString(R.string.language_english));
                                                String derivedConceptName = getDerivedConceptNameFromConceptNamesByLocale(derivedConcept.getDerivedConceptName(), applicationLanguage);
                                                cohortWithDerivedConceptFilters.add(new CohortWithDerivedConceptFilter(cohort, derivedConceptUuid, derivedConceptName));
                                            }
                                        }
                                    } else {
                                        cohortWithDerivedConceptFilters.add(new CohortWithDerivedConceptFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY));
                                    }
                                }
                            } else {
                                derivedConceptUuid = derivedConceptObject.toString();
                                if (!derivedConceptUuid.isEmpty()) {
                                    List<DerivedObservation> derivedObservations = derivedObservationController.getDerivedObservationByDerivedConceptUuid(derivedConceptUuid);
                                    if (derivedObservations.size() > 0) {
                                        List<String> answerValues = new ArrayList<>();
                                        for (DerivedObservation derivedObservation : derivedObservations) {
                                            if (!answerValues.contains(derivedObservation.getValueAsString())) {
                                                cohortWithDerivedConceptFilters.add(new CohortWithDerivedConceptFilter(cohort, derivedConceptUuid, derivedObservation.getValueAsString()));
                                                answerValues.add(derivedObservation.getValueAsString());
                                            }
                                        }
                                    } else {
                                        DerivedConcept derivedConcept = derivedConceptController.getDerivedConceptByUuid(derivedConceptUuid);
                                        if(derivedConcept != null) {
                                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                                            String applicationLanguage = preferences.getString(context.getResources().getString(R.string.preference_app_language), context.getResources().getString(R.string.language_english));
                                            String derivedConceptName = getDerivedConceptNameFromConceptNamesByLocale(derivedConcept.getDerivedConceptName(), applicationLanguage);
                                            cohortWithDerivedConceptFilters.add(new CohortWithDerivedConceptFilter(cohort, derivedConceptUuid, derivedConceptName));
                                        }
                                    }
                                } else {
                                    cohortWithDerivedConceptFilters.add(new CohortWithDerivedConceptFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY));
                                }
                            }
                        }else{
                            cohortWithDerivedConceptFilters.add(new CohortWithDerivedConceptFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY));
                        }
                    }else{
                        cohortWithDerivedConceptFilters.add(new CohortWithDerivedConceptFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY));
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
            Log.e(getClass().getSimpleName(),"Encountered An error while fetching cohorts ",ex);
        } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(),"Encountered An error while fetching muzima settings ",e);
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(),"Encountered a JSON Exception ",e);
        } catch (DerivedObservationController.DerivedObservationFetchException e) {
            Log.e(getClass().getSimpleName(),"Encountered an error while fetching derived observations ",e);
        } catch (NullPointerException e){
            Log.e(getClass().getSimpleName(),"Encountered a null pointer exception while fetching filter setting ",e);
            run();
        } catch (DerivedConceptController.DerivedConceptFetchException e) {
            throw new RuntimeException(e);
        }
    }

    public interface OnDownloadedCohortsLoadedCallback {
        void onCohortsLoaded(List<CohortWithDerivedConceptFilter> cohortWithDerivedConceptFilters);
    }
}
