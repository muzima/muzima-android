/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.patients;


import static com.muzima.utils.Constants.PATIENT_LOAD_PAGE_SIZE;

import android.content.Context;
import android.util.Log;
import com.muzima.api.model.Patient;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.PatientController;
import com.muzima.model.CohortFilter;
import com.muzima.model.PatientFilterPageNumberMap;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatientsLocalSearchAdapter extends PatientAdapterHelper implements MuzimaAsyncTask.OnProgressListener {
    private static final String SEARCH = "search";
    private final PatientController patientController;
    private final List<String> cohortUuids;
    private List<CohortFilter> filters;
    private MuzimaAsyncTask<String, List<Patient>, List<Patient>> backgroundQueryTask;
    private final MuzimaSettingController muzimaSettingController;
    private int patientCount;
    private int nextPageToLoad;
    private boolean isLoading = false;
    private boolean isSubsequentLazyFetchQuery = false;
    private boolean useFuzzySearch;
    private int totalPageCount;
    private Map<Integer, PatientFilterPageNumberMap> totalPageCountMap = new HashMap<>();
    private final List<String> cohortUuidsInActiveConfig;


    public PatientsLocalSearchAdapter(Context context, PatientController patientController,
                                      List<String> cohortUuids, List<CohortFilter> filters,
                                      MuzimaGPSLocation currentLocation, MuzimaSettingController muzimaSettingController, List<String> cohortUuidsInActiveConfig) {
        super(context,patientController, muzimaSettingController);
        this.patientController = patientController;
        this.muzimaSettingController = muzimaSettingController;
        this.cohortUuidsInActiveConfig = cohortUuidsInActiveConfig;
        if (cohortUuids != null){
            this.cohortUuids = cohortUuids;
        } else {
            this.cohortUuids = new ArrayList<>();
        }

        if (filters != null){
            this.filters = filters;
        } else {
            this.filters = new ArrayList<>();
        }

        setCurrentLocation(currentLocation);
        useFuzzySearch = muzimaSettingController.isFuzzySearchEnabled();
    }

    @Override
    public void reloadData() {
        cancelBackgroundTask();
        isSubsequentLazyFetchQuery = false;
        nextPageToLoad = 1;
        runPatientLoadBackgroundQueryTask();
    }

    public void loadNextPage(){
        if(!isLastPage() && !isLoading()) {
            isSubsequentLazyFetchQuery = true;
            nextPageToLoad++;
            runPatientLoadBackgroundQueryTask();
        }
    }

    private void runPatientLoadBackgroundQueryTask(){
        if(!cohortUuids.isEmpty() ) {
            backgroundQueryTask = new PatientLoadBackgroundQueryTask();
            backgroundQueryTask.execute(cohortUuids.toArray(new String[cohortUuids.size()]));
        } else if(filters.size()>0){
            backgroundQueryTask = new PatientLoadBackgroundQueryTask();
            backgroundQueryTask.execute();
        } else {
            backgroundQueryTask = new PatientLoadBackgroundQueryTask();
            backgroundQueryTask.execute(StringUtils.EMPTY);
        }
    }

    public int getTotalPageCount(){
        return totalPageCount;
    }

    public int getTotalPatientCount(){
        return patientCount;
    }

    public boolean isLastPage(){
        return totalPageCount == nextPageToLoad-1;
    }

    public boolean hasLessItemsThanMaxSizeForLoadedPages(){
        return getItemCount() < PATIENT_LOAD_PAGE_SIZE * nextPageToLoad;
    }

    public boolean isLoading(){
        return isLoading;
    }

    public void search(String text) {
        cancelBackgroundTask();
        if(StringUtils.isEmpty(text)) {
            reloadData();
        } else {
            backgroundQueryTask = new PatientLoadBackgroundQueryTask();
            backgroundQueryTask.execute(text, SEARCH);
        }
    }

    public void filterByCohorts(List<String> cohortUuids) {
        cancelBackgroundTask();
        this.cohortUuids.clear();
        this.cohortUuids.addAll(cohortUuids);
        reloadData();
    }

    public void filterByCohortsWithDerivedConceptFilter(List<CohortFilter> cohortFilters) {
        cancelBackgroundTask();
        this.filters.clear();
        this.filters.addAll(cohortFilters);
        reloadData();
    }



    public void cancelBackgroundTask(){
        if(backgroundQueryTask != null){
            backgroundQueryTask.cancel();
        }
    }

    @Override
    public void onProgress(Object o) {
        try {
            onProgressUpdate((List<Patient>) o);
        } catch (ClassCastException e){
            Log.e(getClass().getSimpleName(),"Argument is not a patient list",e);
        }
    }

    private class PatientLoadBackgroundQueryTask extends MuzimaAsyncTask<String, List<Patient>, List<Patient>> {

        @Override
        protected void onPreExecute() {
            isLoading = true;
            if(!isSubsequentLazyFetchQuery) {
                onPreExecuteUpdate();
                setOnProgressListener(PatientsLocalSearchAdapter.this);
            }
        }

        @Override
        protected List<Patient> doInBackground(String... params) {
            List<Patient> patients = null;
            List<Patient> filteredPatients = null;

            if (isSearch(params)) {
                try {
                    if(cohortUuids.size() == 1)
                        return patientController.searchPatientLocally(params[0], cohortUuids.get(0), useFuzzySearch);
                    else
                        return patientController.searchPatientLocally(params[0],null, useFuzzySearch);
                } catch (PatientController.PatientLoadException e) {
                    Log.w(getClass().getSimpleName(), String.format("Exception occurred while searching patients for %s search string." , params[0]), e);
                }
            }

            try {
                int pageSize = PATIENT_LOAD_PAGE_SIZE;
                if (!cohortUuids.isEmpty()) {
                    if(!isSubsequentLazyFetchQuery) {
                        totalPageCountMap.clear();
                        patientCount = 0;
                        totalPageCount = 0;

                        int pageCountForCohort = 0;
                        int uiPageNumber = 0;
                        for (String cohortUuid : cohortUuids) {
                            pageCountForCohort = patientController.countPatients(cohortUuid);
                            patientCount += pageCountForCohort;
                            int pages = new Double(Math.ceil((float) pageCountForCohort / pageSize)).intValue();
                            for (int page = 0; page < pages; page++) {
                                PatientFilterPageNumberMap patientFilterPageNumberMap = new PatientFilterPageNumberMap();
                                patientFilterPageNumberMap.setFilterObject(cohortUuid);
                                patientFilterPageNumberMap.setPageNumber(page);

                                totalPageCountMap.put(++uiPageNumber, patientFilterPageNumberMap);
                                totalPageCount++;
                            }
                        }
                    }
                    PatientFilterPageNumberMap patientFilterPageNumberMap = totalPageCountMap.get(nextPageToLoad);
                    if(patientFilterPageNumberMap != null) {
                        String cohortUuid = (String) patientFilterPageNumberMap.getFilterObject();
                        int page = patientFilterPageNumberMap.getPageNumber();

                        List<Patient> temp = null;

                        if (!isCancelled()) {
                            if (patients == null) {
                                patients = patientController.getPatients(cohortUuid, page, pageSize);
                                if (patients != null) {
                                }
                            } else {
                                temp = patientController.getPatients(cohortUuid, page, pageSize);
                                if (temp != null) {
                                    patients.addAll(temp);
                                }
                            }
                        }
                    }
                } else if(filters.size()>0) {
                    if(filters.size()==1 && filters.get(0).getCohortWithFilter()==null){
                        if(!isSubsequentLazyFetchQuery) {
                            patientCount = patientController.countAllPatients();
                            totalPageCount = new Double(Math.ceil((float) patientCount / pageSize)).intValue();
                        }

                        if(patientCount <= pageSize){
                            patients = patientController.getAllPatients();
                        } else {
                            List<Patient> temp = null;
                            if(!isCancelled()) {
                                if (patients == null) {
                                    patients = patientController.getPatients(nextPageToLoad, pageSize);
                                } else {
                                    temp = patientController.getPatients(nextPageToLoad, pageSize);
                                    if (temp != null) {
                                        patients.addAll(temp);
                                    }
                                }
                            }
                        }

                    } else {
                        if(!isSubsequentLazyFetchQuery) {
                            int pageCountForFilter = 0;
                            int uiPageNumber = 0;
                            totalPageCountMap.clear();
                            patientCount = 0;

                            for(CohortFilter filter: filters){
                                pageCountForFilter = patientController.countPatients(filter.getCohortWithFilter().getCohort().getUuid());
                                patientCount += pageCountForFilter;

                                int pages = new Double(Math.ceil((float) pageCountForFilter / pageSize)).intValue();
                                for (int page = 0; page < pages; page++) {

                                    PatientFilterPageNumberMap patientFilterPageNumberMap = new PatientFilterPageNumberMap();
                                    patientFilterPageNumberMap.setFilterObject(filter);
                                    patientFilterPageNumberMap.setPageNumber(page);
                                    totalPageCountMap.put(++uiPageNumber, patientFilterPageNumberMap);
                                }
                            }
                        }

                        if (!isCancelled()) {
                            PatientFilterPageNumberMap patientFilterPageNumberMap = totalPageCountMap.get(nextPageToLoad);

                            if(patientFilterPageNumberMap != null) {
                                CohortFilter filter = (CohortFilter) patientFilterPageNumberMap.getFilterObject();
                                int page = patientFilterPageNumberMap.getPageNumber();
                                List<String> patientUuids = new ArrayList<>();

                                if (patients == null) {
                                    patients = patientController.getPatients(
                                            filter.getCohortWithFilter().getCohort().getUuid(),
                                            filter.getCohortWithFilter().getDerivedConceptUuid(),
                                            filter.getCohortWithFilter().getDerivedObservationFilter(),
                                            filter.getCohortWithFilter().getConceptUuid(),
                                            filter.getCohortWithFilter().getObservationFilter()
                                            , page, pageSize);

                                    if (patients != null) {
                                        for (Patient patient : patients) {
                                            patientUuids.add(patient.getUuid());
                                        }
                                    }
                                } else {

                                    List<Patient> temp = patientController.getPatients(filter.getCohortWithFilter().getCohort().getUuid(),
                                            filter.getCohortWithFilter().getDerivedConceptUuid(),
                                            filter.getCohortWithFilter().getDerivedObservationFilter(),
                                            filter.getCohortWithFilter().getConceptUuid(),
                                            filter.getCohortWithFilter().getObservationFilter(),
                                            page, pageSize);
                                    if (temp != null) {
                                        for (Patient patient : temp) {
                                            if (!patientUuids.contains(patient.getUuid())) {
                                                patientUuids.add(patient.getUuid());
                                                patients.add(patient);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (cohortUuidsInActiveConfig == null || cohortUuidsInActiveConfig.isEmpty()) {
                        if (!isSubsequentLazyFetchQuery) {
                            patientCount = patientController.countAllPatients();
                            totalPageCount = new Double(Math.ceil((float) patientCount / pageSize)).intValue();
                        }
                        if (patientCount <= pageSize) {
                            patients = patientController.getAllPatients();
                        } else {
                            List<Patient> temp = null;
                            if (!isCancelled()) {
                                if (patients == null) {
                                    patients = patientController.getPatients(nextPageToLoad, pageSize);
                                } else {
                                    temp = patientController.getPatients(nextPageToLoad, pageSize);
                                    if (temp != null) {
                                        patients.addAll(temp);
                                    }
                                }
                            }
                        }
                    }else{
                        if(!isSubsequentLazyFetchQuery) {
                            totalPageCountMap.clear();
                            patientCount = 0;
                            totalPageCount = 0;

                            int pageCountForCohort = 0;
                            int uiPageNumber = 0;
                            for (String cohortUuid : cohortUuidsInActiveConfig) {
                                pageCountForCohort = patientController.countPatients(cohortUuid);
                                patientCount += pageCountForCohort;
                                int pages = new Double(Math.ceil((float) pageCountForCohort / pageSize)).intValue();
                                for (int page = 0; page < pages; page++) {
                                    PatientFilterPageNumberMap patientFilterPageNumberMap = new PatientFilterPageNumberMap();
                                    patientFilterPageNumberMap.setFilterObject(cohortUuid);
                                    patientFilterPageNumberMap.setPageNumber(page);

                                    totalPageCountMap.put(++uiPageNumber, patientFilterPageNumberMap);
                                    totalPageCount++;
                                }
                            }
                        }

                        PatientFilterPageNumberMap patientFilterPageNumberMap = totalPageCountMap.get(nextPageToLoad);
                        if(patientFilterPageNumberMap != null) {
                            String cohortUuid = (String) patientFilterPageNumberMap.getFilterObject();
                            int page = patientFilterPageNumberMap.getPageNumber();

                            List<Patient> temp = null;

                            if (!isCancelled()) {
                                if (patients == null) {
                                    patients = patientController.getPatients(cohortUuid, page, pageSize);
                                } else {
                                    temp = patientController.getPatients(cohortUuid, page, pageSize);
                                    if (temp != null) {
                                        patients.addAll(temp);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (PatientController.PatientLoadException e) {
                Log.e(getClass().getSimpleName(), "Exception occurred while fetching patients", e);
            }

            if(patients == null) patients = new ArrayList<>();
            List<String> tags = patientController.getSelectedTagUuids();
            filteredPatients = patientController.filterPatientByTags(patients,tags);

            try {
                List<String> tagsToHidePatients = muzimaSettingController.getTagsForPatientsToHide();
                List<String> nonSelectedTagsToHidePatients = new ArrayList<>();
                List<String> tagsToRetain = patientController.getSelectedTagUuids();
                for(String tagToHide: tagsToHidePatients){
                    if(!tagsToRetain.contains(tagToHide))
                        nonSelectedTagsToHidePatients.add(tagToHide);
                }
                filteredPatients = patientController.removePatientsWithTags(filteredPatients, nonSelectedTagsToHidePatients);
            } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
                Log.e(getClass().getSimpleName(), "Error tags list for patient sto hide setting", e);
            }
            return filteredPatients;
        }

        private boolean isSearch(String[] params) {
            if(params != null)
                return params.length == 2 && SEARCH.equals(params[1]);
            else
                return false;
        }

        @Override
        protected void onPostExecute(List<Patient> patients) {
            isLoading = false;
            onPostExecuteUpdate(patients, isSubsequentLazyFetchQuery);
        }

        @Override
        protected void onBackgroundError(Exception e) {
            Log.e(getClass().getSimpleName(), "Error while running background task",e);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }


}
