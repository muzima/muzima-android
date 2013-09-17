package com.muzima.controller;

import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.api.service.ConceptService;
import com.muzima.api.service.ObservationService;
import com.muzima.model.observation.ConceptWithObservations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static java.util.Arrays.asList;

public class ObservationController {

    private ObservationService observationService;
    private ConceptService conceptService;
    private static final int ORDER_DESCENDING = -1;

    public ObservationController(ObservationService observationService, ConceptService conceptService) {
        this.observationService = observationService;
        this.conceptService = conceptService;
    }

    public List<Observation> getObservationsByDate(String patientUuid) throws LoadObservationException {
        try {
            List<Observation> observationsByPatient = observationService.getObservationsByPatient(patientUuid);
            Collections.sort(observationsByPatient, new Comparator<Observation>() {
                @Override
                public int compare(Observation observation, Observation observation2) {
                    return ORDER_DESCENDING * observation.getObservationDatetime().compareTo(observation2.getObservationDatetime());
                }
            });
            return observationsByPatient;
        } catch (IOException e) {
            throw new LoadObservationException(e);
        }
    }

    public List<ConceptWithObservations> getConceptWithObservations(String patientUuid) throws LoadObservationException {
        try {
            List<Observation> observationsByPatient = observationService.getObservationsByPatient(patientUuid);

            Map<String, ConceptWithObservations> conceptWithObservationsMap = new HashMap<String, ConceptWithObservations>();
            for (Observation observation : observationsByPatient) {
                Concept concept = observation.getConcept();
                ConceptWithObservations conceptWithObservations = conceptWithObservationsMap.get(concept.getUuid());
                if (conceptWithObservations == null) {
                    conceptWithObservations = new ConceptWithObservations();
                    conceptWithObservations.setConcept(conceptService.getConceptByUuid(concept.getUuid()));
                    conceptWithObservationsMap.put(concept.getUuid(), conceptWithObservations);
                }
                conceptWithObservations.addObservation(observation);
            }

            return new ArrayList<ConceptWithObservations>(conceptWithObservationsMap.values());
        } catch (IOException e) {
            throw new LoadObservationException(e);
        }
    }

    public void replaceObservations(List<String> patientUuids, List<Observation> allObservations) throws LoadObservationException {
        try {
            for (String patientUuid : patientUuids) {
                List<Observation> observationsByPatient = observationService.getObservationsByPatient(patientUuid);
                observationService.deleteObservations(observationsByPatient);
            }
            observationService.saveObservations(allObservations);
        } catch (IOException e) {
            throw new LoadObservationException(e);
        }
    }

    public List<Observation> downloadObservations(String patientUuid, String conceptUuid) throws DownloadObservationException {
        try {
            return observationService.downloadObservationsByPatientAndConcept(patientUuid, conceptUuid);
        } catch (IOException e) {
            throw new DownloadObservationException(e);
        }
    }

    public List<Observation> searchObservations(String term, String patientUuid) throws LoadObservationException {
        try {
            return observationService.searchObservations(patientUuid, term);
        } catch (IOException e) {
            throw new LoadObservationException(e);
        }
    }


    public class LoadObservationException extends Throwable {
        public LoadObservationException(Throwable e) {
            super(e);
        }
    }

    public class DownloadObservationException extends Throwable {
        public DownloadObservationException(Throwable e) {
            super(e);
        }
    }

}
