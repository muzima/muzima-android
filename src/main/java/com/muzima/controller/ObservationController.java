package com.muzima.controller;

import com.muzima.api.model.Observation;
import com.muzima.api.service.ObservationService;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ObservationController {

    private ObservationService observationService;

    public ObservationController(ObservationService observationService) {
        this.observationService = observationService;
    }

    public List<Observation> getObservationsByDate(String patientUuid) throws LoadObservationException {
        try {
            List<Observation> observationsByPatient = observationService.getObservationsByPatient(patientUuid);
            Collections.sort(observationsByPatient, new Comparator<Observation>() {
                @Override
                public int compare(Observation observation, Observation observation2) {
                    return observation.getObservationDate().compareTo(observation2.getObservationDate());
                }
            });
            return observationsByPatient;
        } catch (IOException e) {
            throw new LoadObservationException(e);
        }
    }


    public class LoadObservationException extends Throwable {
        public LoadObservationException(IOException e) {
            super(e);
        }
    }
}
