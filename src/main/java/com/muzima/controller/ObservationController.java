package com.muzima.controller;

import com.muzima.api.model.*;
import com.muzima.api.service.ConceptService;
import com.muzima.api.service.EncounterService;
import com.muzima.api.service.ObservationService;
import com.muzima.model.observation.Concepts;
import com.muzima.model.observation.Encounters;
import com.muzima.utils.CustomColor;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.AuthProvider;
import java.security.Provider;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObservationController {

    private ObservationService observationService;
    private ConceptService conceptService;
    private EncounterService encounterService;
    private Map<String, Integer> conceptColors;
    private Map<String, Integer> encounterColors;

    public ObservationController(ObservationService observationService, ConceptService conceptService, EncounterService encounterService) {
        this.observationService = observationService;
        this.conceptService = conceptService;
        this.encounterService = encounterService;
        conceptColors = new HashMap<String, Integer>();
        encounterColors = new HashMap<String, Integer>();
    }

    public Concepts getConceptWithObservations(String patientUuid) throws LoadObservationException {
        try {
            return groupByConcepts(observationService.getObservationsByPatient(patientUuid));
        } catch (IOException e) {
            throw new LoadObservationException(e);
        }
    }

    private void inflateConcepts(List<Observation> observationsByPatient) throws IOException {
        Map<String, Concept> conceptCache = new HashMap<String, Concept>();

        for (Observation observation : observationsByPatient) {
            Concept concept = observation.getConcept();
            String conceptUuid = concept.getUuid();
            if (!conceptCache.containsKey(conceptUuid)) {
                Concept conceptByUuid = conceptService.getConceptByUuid(conceptUuid);
                conceptCache.put(conceptUuid, conceptByUuid);
            }
            observation.setConcept(conceptCache.get(conceptUuid));
        }
    }

    private void inflateEncounters(List<Observation> observationsByPatient) throws IOException {
        Map<String, Encounter> encounterCache = new HashMap<String, Encounter>();

        for (Observation observation : observationsByPatient) {
            Encounter encounter = observation.getEncounter();
            String encounterUuid = encounter.getUuid();
            if (!encounterCache.containsKey(encounterUuid)) {
                Encounter fullEncounter = getEncounterForNullEncounterUuid();
                if (encounterUuid!=null)
                    fullEncounter = encounterService.getEncounterByUuid(encounterUuid);
                encounterCache.put(encounterUuid, fullEncounter);
            }
            observation.setEncounter(encounterCache.get(encounterUuid));
        }
    }

    private Encounter getEncounterForNullEncounterUuid() {
        final Date finalDate = new Date();
        final PersonName personName = new PersonName();
        personName.setGivenName("Observations");
        final Person person = new Person();
        person.addName(personName);
        final Location location = new Location();
        location.setName("without encounter");
        return new Encounter(){{
            setEncounterDatetime(finalDate);
            setProvider(person);
            setLocation(location);
        }};
    }

    public int getConceptColor(String uuid) {
        if (!conceptColors.containsKey(uuid)) {
            conceptColors.put(uuid, CustomColor.getOrderedColor(conceptColors.size()));
        }
        return conceptColors.get(uuid);
    }

    public int getEncounterColor(String encounterTypeUuid) {
        if (!encounterColors.containsKey(encounterTypeUuid)) {
            encounterColors.put(encounterTypeUuid, CustomColor.getOrderedColor(encounterColors.size()));
        }
        return encounterColors.get(encounterTypeUuid);
    }

    public void replaceObservations(List<String> patientUuids, List<Observation> allObservations) throws ReplaceObservationException {
        try {
            for (String patientUuid : patientUuids) {
                List<Observation> observationsByPatient = observationService.getObservationsByPatient(patientUuid);
                observationService.deleteObservations(observationsByPatient);
            }
            observationService.saveObservations(allObservations);
        } catch (IOException e) {
            throw new ReplaceObservationException(e);
        }
    }

    public List<Observation> downloadObservations(String patientUuid, String conceptUuid) throws DownloadObservationException {
        try {
            return observationService.downloadObservationsByPatientAndConcept(patientUuid, conceptUuid);
        } catch (IOException e) {
            throw new DownloadObservationException(e);
        }
    }

    public Concepts searchObservationsGroupedByConcepts(String term, String patientUuid) throws LoadObservationException {
        try {
            return groupByConcepts(observationService.searchObservations(patientUuid, term));
        } catch (IOException e) {
            throw new LoadObservationException(e);
        }
    }

    private Concepts groupByConcepts(List<Observation> observations) throws IOException {
        inflateConcepts(observations);
        return new Concepts(observations);
    }

    public Encounters getEncountersWithObservations(String patientUuid) throws LoadObservationException {
        try {
            return groupByEncounters(observationService.getObservationsByPatient(patientUuid));
        } catch (IOException e) {
            throw new LoadObservationException(e);
        }
    }

    private Encounters groupByEncounters(List<Observation> observationsByPatient) throws IOException {
        inflateConcepts(observationsByPatient);
        inflateEncounters(observationsByPatient);
        return new Encounters(observationsByPatient);
    }

    public Encounters searchObservationsGroupedByEncounter(String term, String patientUuid) throws LoadObservationException {
        try {
            return groupByEncounters(observationService.searchObservations(patientUuid, term));
        } catch (IOException e) {
            throw new LoadObservationException(e);
        }
    }


    public static class LoadObservationException extends Throwable {
        public LoadObservationException(Throwable e) {
            super(e);
        }
    }

    public static class ReplaceObservationException extends Throwable {
        public ReplaceObservationException(Throwable e) {
            super(e);
        }
    }

    public static class DownloadObservationException extends Throwable {
        public DownloadObservationException(Throwable e) {
            super(e);
        }
    }

}
