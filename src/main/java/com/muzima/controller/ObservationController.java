package com.muzima.controller;

import com.muzima.api.model.Observation;
import com.muzima.api.service.ConceptService;
import com.muzima.api.service.ObservationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ObservationController {

    private ObservationService observationService;
    private static final int ORDER_DESCENDING = -1;

    public ObservationController(ObservationService observationService) {
        this.observationService = observationService;
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
