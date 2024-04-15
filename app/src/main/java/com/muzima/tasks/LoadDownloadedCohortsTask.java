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

import static com.muzima.util.Constants.ServerSettings.COHORT_FILTER_CONCEPT_MAP;
import static com.muzima.util.Constants.ServerSettings.COHORT_FILTER_DERIVED_CONCEPT_MAP;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.Concept;
import com.muzima.api.model.DerivedConcept;
import com.muzima.api.model.DerivedObservation;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.controller.CohortController;
import com.muzima.controller.ConceptController;
import com.muzima.controller.DerivedConceptController;
import com.muzima.controller.DerivedObservationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.model.CohortWithFilter;
import com.muzima.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.muzima.utils.ConceptUtils;

public class LoadDownloadedCohortsTask implements Runnable {

    private Context context;
    private OnDownloadedCohortsLoadedCallback cohortsLoadedCallback;

    public LoadDownloadedCohortsTask(Context context, OnDownloadedCohortsLoadedCallback cohortsLoadedCallback) {
        this.context = context;
        this.cohortsLoadedCallback = cohortsLoadedCallback;
    }

    @Override
    public void run() {
        List<CohortWithFilter> cohortWithFilters = new ArrayList<>();
        try {
            List<String> interventions = new ArrayList<String>(){{
                add("4b479a6c-4276-45a1-b785-ecbc7dc59ff1");
                add("1bd47ba9-b6ff-4b4c-ba26-f5b86498d738");
                add("9e928864-b7d2-445d-9856-cb7c9a0632dd");
                add("46e6c352-bddb-4191-8d1e-40380aa1a346");
                add("379e2aa5-b750-4b08-af13-cd0b9795eca7");
            }};

            MuzimaSetting cohortFilterDerivedConceptMapSetting = null;
            MuzimaSetting cohortFilterConceptMapSetting = null;
            try{
                cohortFilterDerivedConceptMapSetting = ((MuzimaApplication) context.getApplicationContext()).getMuzimaSettingController().getSettingByProperty(COHORT_FILTER_DERIVED_CONCEPT_MAP);
                cohortFilterConceptMapSetting = ((MuzimaApplication) context.getApplicationContext()).getMuzimaSettingController().getSettingByProperty(COHORT_FILTER_CONCEPT_MAP);

            } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
                Log.e(getClass().getSimpleName(),"Encountered An error while fetching muzima settings ",e);
            }

            DerivedObservationController derivedObservationController = ((MuzimaApplication) context.getApplicationContext()).getDerivedObservationController();
            DerivedConceptController derivedConceptController = ((MuzimaApplication) context.getApplicationContext()).getDerivedConceptController();
            ConceptController conceptController = ((MuzimaApplication) context.getApplicationContext()).getConceptController();
            JSONObject jsonCohortObject = new JSONObject();
            if(cohortFilterConceptMapSetting != null) {
                String obsSettingValue = cohortFilterConceptMapSetting.getValueString();
                if (obsSettingValue != null) {
                    jsonCohortObject = new JSONObject(obsSettingValue);
                }
            }

            List<String> cohortUuids = new ArrayList<>();
            for (Cohort cohort : ((MuzimaApplication) context.getApplicationContext()).getCohortController()
                    .getCohorts()) {
                if (((MuzimaApplication) context.getApplicationContext()).getCohortController().isDownloaded(cohort)) {
                    if(cohortFilterDerivedConceptMapSetting != null && cohortFilterDerivedConceptMapSetting.getValueString() != null) {
                        String settingValue = cohortFilterDerivedConceptMapSetting.getValueString();
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
                                        if(interventions.contains(derivedConceptUuid)){
                                            DerivedConcept derivedConcept = derivedConceptController.getDerivedConceptByUuid(derivedConceptUuid);
                                            if (derivedConcept != null) {
                                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                                                String applicationLanguage = preferences.getString(context.getResources().getString(R.string.preference_app_language), context.getResources().getString(R.string.language_portuguese));
                                                String derivedConceptName = ConceptUtils.getDerivedConceptNameFromConceptNamesByLocale(derivedConcept.getDerivedConceptName(), applicationLanguage);
                                                cohortWithFilters.add(new CohortWithFilter(cohort, derivedConceptUuid, derivedConceptName, StringUtils.EMPTY, StringUtils.EMPTY));
                                            }
                                        } else {
                                            List<DerivedObservation> derivedObservations = derivedObservationController.getDerivedObservationByDerivedConceptUuid(derivedConceptUuid);
                                            if (derivedObservations.size() > 0) {
                                                List<String> answerValues = new ArrayList<>();
                                                for (DerivedObservation derivedObservation : derivedObservations) {
                                                    if (!answerValues.contains(derivedObservation.getValueAsString())) {
                                                        cohortWithFilters.add(new CohortWithFilter(cohort, derivedConceptUuid, derivedObservation.getValueAsString(), StringUtils.EMPTY , StringUtils.EMPTY));
                                                        answerValues.add(derivedObservation.getValueAsString());
                                                    }
                                                }
                                            } else {
                                                DerivedConcept derivedConcept = derivedConceptController.getDerivedConceptByUuid(derivedConceptUuid);
                                                if (derivedConcept != null) {
                                                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                                                    String applicationLanguage = preferences.getString(context.getResources().getString(R.string.preference_app_language), context.getResources().getString(R.string.language_portuguese));
                                                    String derivedConceptName = ConceptUtils.getDerivedConceptNameFromConceptNamesByLocale(derivedConcept.getDerivedConceptName(), applicationLanguage);
                                                    cohortWithFilters.add(new CohortWithFilter(cohort, derivedConceptUuid, derivedConceptName, StringUtils.EMPTY, StringUtils.EMPTY));
                                                }
                                            }
                                        }
                                    } else {
                                        cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY));
                                    }
                                }
                            } else {
                                derivedConceptUuid = derivedConceptObject.toString();
                                if (!derivedConceptUuid.isEmpty()) {
                                    if(interventions.contains(derivedConceptUuid)){
                                        DerivedConcept derivedConcept = derivedConceptController.getDerivedConceptByUuid(derivedConceptUuid);
                                        if (derivedConcept != null) {
                                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                                            String applicationLanguage = preferences.getString(context.getResources().getString(R.string.preference_app_language), context.getResources().getString(R.string.language_portuguese));
                                            String derivedConceptName = ConceptUtils.getDerivedConceptNameFromConceptNamesByLocale(derivedConcept.getDerivedConceptName(), applicationLanguage);
                                            cohortWithFilters.add(new CohortWithFilter(cohort, derivedConceptUuid, derivedConceptName, StringUtils.EMPTY, StringUtils.EMPTY));
                                        }
                                    } else {
                                        List<DerivedObservation> derivedObservations = derivedObservationController.getDerivedObservationByDerivedConceptUuid(derivedConceptUuid);
                                        if (derivedObservations.size() > 0) {
                                            List<String> answerValues = new ArrayList<>();
                                            for (DerivedObservation derivedObservation : derivedObservations) {
                                                if (!answerValues.contains(derivedObservation.getValueAsString())) {
                                                    cohortWithFilters.add(new CohortWithFilter(cohort, derivedConceptUuid, derivedObservation.getValueAsString(), StringUtils.EMPTY, StringUtils.EMPTY));
                                                    answerValues.add(derivedObservation.getValueAsString());
                                                }
                                            }
                                        } else {
                                            DerivedConcept derivedConcept = derivedConceptController.getDerivedConceptByUuid(derivedConceptUuid);
                                            if (derivedConcept != null) {
                                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                                                String applicationLanguage = preferences.getString(context.getResources().getString(R.string.preference_app_language), context.getResources().getString(R.string.language_portuguese));
                                                String derivedConceptName = ConceptUtils.getDerivedConceptNameFromConceptNamesByLocale(derivedConcept.getDerivedConceptName(), applicationLanguage);
                                                cohortWithFilters.add(new CohortWithFilter(cohort, derivedConceptUuid, derivedConceptName,StringUtils.EMPTY ,StringUtils.EMPTY));
                                            }
                                        }
                                    }
                                } else {
                                    cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY,StringUtils.EMPTY ,StringUtils.EMPTY));
                                }
                            }
                        }else{
                            if(!jsonCohortObject.has(cohort.getUuid())) {
                                cohortUuids.add(cohort.getUuid());
                                cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY));
                            }
                        }
                    }else{
                        if(!jsonCohortObject.has(cohort.getUuid())) {
                            cohortUuids.add(cohort.getUuid());
                            cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY));
                        }
                    }

                    if(cohortFilterConceptMapSetting != null) {
                        String obsSettingValue = cohortFilterConceptMapSetting.getValueString();
                        if (obsSettingValue != null) {
                            String conceptUuid = "";
                            Object conceptObject = null;
                            JSONObject jsonObject = new JSONObject(obsSettingValue);
                            if (jsonObject.has(cohort.getUuid())) {
                                conceptObject = jsonObject.get(cohort.getUuid());
                                if (conceptObject != null && conceptObject instanceof JSONArray) {
                                    JSONArray jsonArray = (JSONArray) conceptObject;
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        conceptUuid = jsonArray.get(i).toString();
                                        if (!conceptUuid.isEmpty()) {
                                            if (conceptUuid.equals("e3f3bcea-9e2f-4aec-976f-0290bdbfffb8")) {
                                                Concept concept = conceptController.getConceptByUuid(conceptUuid);
                                                if (concept != null) {
                                                    cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY, conceptUuid, "Próximos 10 dias"));
                                                    cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY, conceptUuid, "Este mês"));
                                                    cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY, conceptUuid, "Próximo mês"));
                                                }
                                            } else {
                                                cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY));
                                            }
                                        } else {
                                            cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY));
                                        }
                                    }
                                } else {
                                    conceptUuid = conceptObject.toString();
                                    if (!conceptUuid.isEmpty()) {
                                        if (conceptUuid.equals("e3f3bcea-9e2f-4aec-976f-0290bdbfffb8")) {
                                            Concept concept = conceptController.getConceptByUuid(conceptUuid);
                                            if (concept != null) {
                                                cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY, conceptUuid, "Próximos 10 dias"));
                                                cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY, conceptUuid, "Este mês"));
                                                cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY, conceptUuid, "Próximo mês"));
                                            }
                                        } else {
                                            cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY));
                                        }
                                    } else {
                                        cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY));
                                    }
                                }
                            } else {
                                if(!cohortUuids.contains(cohort.getUuid()))
                                    cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY));
                            }
                        } else {
                            if(!cohortUuids.contains(cohort.getUuid()))
                                cohortWithFilters.add(new CohortWithFilter(cohort, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY));
                        }
                    }
                }
            }

            Collections.sort(cohortWithFilters, new Comparator<CohortWithFilter>() {
                @Override
                public int compare(CohortWithFilter o1, CohortWithFilter o2) {
                    return o1.getCohort().getName().compareTo(o2.getCohort().getName());
                }
            });
        } catch (CohortController.CohortFetchException ex) {
            Log.e(getClass().getSimpleName(),"Encountered An error while fetching cohorts ",ex);
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(),"Encountered a JSON Exception ",e);
        } catch (DerivedObservationController.DerivedObservationFetchException e) {
            Log.e(getClass().getSimpleName(),"Encountered an error while fetching derived observations ",e);
        } catch (DerivedConceptController.DerivedConceptFetchException e) {
            Log.e(getClass().getSimpleName(),"Encountered an error while fetching derived concepts ",e);
        } catch (ConceptController.ConceptFetchException e) {
            Log.e(getClass().getSimpleName(),"Encountered an error while fetching concepts",e);
        } finally {
            cohortsLoadedCallback.onCohortsLoaded(cohortWithFilters);
        }
    }

    public interface OnDownloadedCohortsLoadedCallback {
        void onCohortsLoaded(List<CohortWithFilter> cohortWithFilters);
    }
}
