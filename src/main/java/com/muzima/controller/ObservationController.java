package com.muzima.controller;

import com.muzima.api.model.*;
import com.muzima.api.service.ConceptService;
import com.muzima.api.service.EncounterService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.ObservationService;
import com.muzima.model.observation.Concepts;
import com.muzima.model.observation.Encounters;
import com.muzima.service.SntpService;
import com.muzima.utils.CustomColor;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.muzima.api.model.APIName.DOWNLOAD_OBSERVATIONS;

public class ObservationController {

    private ObservationService observationService;
    private ConceptService conceptService;
    private EncounterService encounterService;
    private LastSyncTimeService lastSyncTimeService;
    private SntpService sntpService;
    private Map<String, Integer> conceptColors;

    public ObservationController(ObservationService observationService, ConceptService conceptService,
                                 EncounterService encounterService, LastSyncTimeService lastSyncTimeService,
                                 SntpService sntpService) {
        this.observationService = observationService;
        this.conceptService = conceptService;
        this.encounterService = encounterService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
        conceptColors = new HashMap<String, Integer>();
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
        encounterCache.put(null, getEncounterForNullEncounterUuid());

        for (Observation observation : observationsByPatient) {
            Encounter encounter = observation.getEncounter();
            String encounterUuid = encounter.getUuid();
            if (!encounterCache.containsKey(encounterUuid)) {
                Encounter fullEncounter = encounterService.getEncounterByUuid(encounterUuid);
                encounterCache.put(encounterUuid, fullEncounter);
            }
            observation.setEncounter(encounterCache.get(encounterUuid));
        }
    }

    private Encounter getEncounterForNullEncounterUuid() {
        final Person person = new Person();
        person.addName(new PersonName());
        final Location location = new Location();
        location.setName("");
        return new Encounter(){{
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

    public void replaceObservations(List<Observation> allObservations) throws ReplaceObservationException {
        try {
            observationService.updateObservations(allObservations);
        } catch (IOException e) {
            throw new ReplaceObservationException(e);
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

    public List<Observation> downloadObservationsByPatientUuidsAndConceptUuids(List<String> patientUuids, List<String> conceptUuids) throws DownloadObservationException {
        try {
            String paramSignature = buildParamSignature(patientUuids, conceptUuids);
            Date lastSyncTime = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_OBSERVATIONS, paramSignature);
            List<Observation> observations = observationService.downloadObservations(patientUuids, conceptUuids, lastSyncTime);
            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_OBSERVATIONS, sntpService.getLocalTime(), paramSignature);
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
            return observations;
        } catch (IOException e) {
            throw new DownloadObservationException(e);
        }
    }

    private String buildParamSignature(List<String> patientUuids, List<String> conceptUuids) {
        String paramSignature = StringUtils.join(patientUuids, ",");
        paramSignature += "|";
        paramSignature += StringUtils.join(conceptUuids, ",");
        return paramSignature;
    }

    public void saveObservations(List<Observation> observations) throws SaveObservationException {
        try {
            observationService.saveObservations(observations);
        } catch (IOException e) {
            throw new SaveObservationException(e);
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

    public static class SaveObservationException extends Throwable {
        public SaveObservationException(Throwable e) {
            super(e);
        }
    }
}
