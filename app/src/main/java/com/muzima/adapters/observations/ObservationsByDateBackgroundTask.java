/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.muzima.R;
import com.muzima.api.model.Concept;
import com.muzima.api.model.ConceptName;
import com.muzima.api.model.DerivedObservation;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;
import com.muzima.controller.DerivedObservationController;
import com.muzima.controller.ObservationController;
import com.muzima.utils.ConceptUtils;
import com.muzima.utils.MuzimaPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AsyncTask Class that orchestrate a background non ui thread that loads
 * Observation data by concept from local storage.
 *
 * @see AsyncTask
 * @see com.muzima.api.model.Observation
 * @see Concept
 */
class ObservationsByDateBackgroundTask extends AsyncTask<Void, List<String>, List<String>> {

    private final ObservationByDateAdapter observationByDateAdapter;
    private final ObservationController observationController;
    private final DerivedObservationController derivedObservationController;
    private final String patientUuid;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    Map<String, List<Observation>> datesObservations = new LinkedHashMap<>();
    private String applicationLanguage;


    public ObservationsByDateBackgroundTask(ObservationByDateAdapter observationByDateAdapter, ObservationController observationController, String patientUuid, DerivedObservationController derivedObservationController, Context context) {
        this.observationByDateAdapter = observationByDateAdapter;
        this.observationController = observationController;
        this.derivedObservationController = derivedObservationController;
        this.patientUuid = patientUuid;
        this.applicationLanguage = MuzimaPreferences.getStringPreference(context, context.getResources().getString(R.string.preference_app_language), context.getResources().getString(R.string.language_english));
    }

    @Override
    protected void onPreExecute() {
        if (observationByDateAdapter.getBackgroundListQueryTaskListener() != null) {
            observationByDateAdapter.getBackgroundListQueryTaskListener().onQueryTaskStarted();
        }
    }

    @Override
    protected List<String> doInBackground(Void... params) {
        List<String> dates = new ArrayList();
        try {
            List<Observation> observations = observationController.getObservationsByPatient(patientUuid);
            List<DerivedObservation> derivedObservations = derivedObservationController.getDerivedObservationByPatientUuid(patientUuid);
            for (DerivedObservation derivedObservation : derivedObservations) {
                Observation observation = new Observation();

                List<ConceptName> conceptNames = new ArrayList<>();
                ConceptName conceptName = new ConceptName();
                conceptName.setName(ConceptUtils.getDerivedConceptNameFromConceptNamesByLocale(derivedObservation.getDerivedConcept().getDerivedConceptName(), applicationLanguage));
                conceptName.setLocale(applicationLanguage);
                conceptNames.add(conceptName);

                Concept concept = new Concept();
                concept.setConceptUuid(derivedObservation.getDerivedConcept().getDerivedConceptUuid());
                concept.setConceptNames(new ArrayList<>(conceptNames));
                concept.setConceptType(derivedObservation.getDerivedConcept().getConceptType());

                observation.setObsUuid(derivedObservation.getDerivedObservationUuid());
                observation.setPerson(derivedObservation.getPerson());
                observation.setConcept(concept);
                observation.setValueCoded(derivedObservation.getValueCoded());
                observation.setValueDatetime(derivedObservation.getValueDatetime());
                observation.setValueNumeric(derivedObservation.getValueNumeric());
                observation.setValueText(derivedObservation.getValueText());
                observation.setValueBoolean(derivedObservation.isValueBoolean());
                observation.setObservationDatetime(derivedObservation.getDateCreated());

                observations.add(observation);
            }

            Collections.sort(observations, obsDateTimeComparator);
            List<Observation> observationList = new ArrayList<>();
            for (Observation observation : observations) {
                if (!isCancelled() && observation.getObservationDatetime() != null) {
                    String formattedDate = dateFormat.format(observation.getObservationDatetime());
                    if(dates == null){
                        observationList.add(observation);
                        dates.add(formattedDate);
                    } else if(!dates.contains(formattedDate)){
                        if(dates.size()>0) {
                            datesObservations.put(dates.get(dates.size() - 1), observationList);
                            observationList = new ArrayList<>();
                        }
                        observationList.add(observation);
                        dates.add(formattedDate);
                    }else{
                        observationList.add(observation);
                    }
                }
            }
            if(observationList.size()>0 && dates.size()>0){
                datesObservations.put(dates.get(dates.size() - 1), observationList);
            }
        } catch (ObservationController.LoadObservationException e) {
            Log.w("Observations", String.format("Exception while loading observations for %s."), e);
        } catch (DerivedObservationController.DerivedObservationFetchException e) {
            Log.w(getClass().getSimpleName(), "Exception while loading derived observations for %s.", e);
        }
        return dates;
    }

    private final Comparator<Observation> obsDateTimeComparator = (lhs, rhs) -> {
        if (lhs.getObservationDatetime()==null)
            return -1;
        if (rhs.getObservationDatetime()==null)
            return 1;
        return -(lhs.getObservationDatetime()
                .compareTo(rhs.getObservationDatetime()));
    };


    @Override
    protected void onPostExecute(List<String> dates) {
        if (dates != null) {
            observationByDateAdapter.clear();
            observationByDateAdapter.add(dates,datesObservations);
            observationByDateAdapter.notifyDataSetChanged();
        }

        if (observationByDateAdapter.getBackgroundListQueryTaskListener() != null) {
            observationByDateAdapter.getBackgroundListQueryTaskListener().onQueryTaskFinish();
        }
    }

    private final Comparator<Encounter> encountersDateTimeComparator = (lhs, rhs) -> {
        if (lhs.getEncounterDatetime()==null)
            return -1;
        if (rhs.getEncounterDatetime()==null)
            return 1;
        return -(lhs.getEncounterDatetime()
                .compareTo(rhs.getEncounterDatetime()));
    };
}
