/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.controller;

import com.muzima.api.model.Concept;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.Location;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonName;
import com.muzima.api.service.ConceptService;
import com.muzima.api.service.EncounterService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.ObservationService;
import com.muzima.model.observation.Concepts;
import com.muzima.model.observation.Encounters;
import com.muzima.service.SntpService;
import com.muzima.utils.CustomColor;
import com.muzima.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.muzima.api.model.APIName.DOWNLOAD_OBSERVATIONS;
import static com.muzima.util.Constants.UUID_SEPARATOR;
import static com.muzima.util.Constants.UUID_TYPE_SEPARATOR;
import static java.util.Arrays.asList;

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
    public int getObservationsCountByPatient(String patientUuid) throws IOException {
        return observationService.getObservationsByPatient(patientUuid).size();
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
    public List<Observation> getObservationsByPatient(String patientUuid)  throws LoadObservationException{
        try {
            List<Observation> observations = observationService.getObservationsByPatient(patientUuid);
            inflateConcepts(observations);
            return observations;
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
            List<String> allConceptsUuids = conceptUuids;
            List<String> allPatientsUuids = patientUuids;
            String paramSignature = buildParamSignature(allPatientsUuids, allConceptsUuids);
            Date lastSyncTime = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_OBSERVATIONS, paramSignature);
            List<Observation> observations = new ArrayList<Observation>();
            if (hasExactCallBeenMadeBefore(lastSyncTime)) {
                observations.addAll(observationService.downloadObservations(patientUuids, conceptUuids, lastSyncTime));
            } else {
                LastSyncTime fullLastSyncTimeInfo = lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_OBSERVATIONS);
                if (isFirstCallToDownloadObservationsEver(fullLastSyncTimeInfo)) {
                    observations.addAll(observationService.downloadObservations(patientUuids, conceptUuids, null));
                } else {
                    String[] parameterSplit = fullLastSyncTimeInfo.getParamSignature().split(UUID_TYPE_SEPARATOR, -1);
                    List<String> knownPatientsUuid = asList(parameterSplit[0].split(UUID_SEPARATOR));
                    List<String> newPatientsUuids = getNewUuids(patientUuids, knownPatientsUuid);
                    List<String> knownConceptsUuid = asList(parameterSplit[1].split(UUID_SEPARATOR));
                    List<String> newConceptsUuids = getNewUuids(conceptUuids, knownConceptsUuid);
                    allConceptsUuids = getAllUuids(knownConceptsUuid, newConceptsUuids);
                    allPatientsUuids = getAllUuids(knownPatientsUuid, newPatientsUuids);
                    paramSignature = buildParamSignature(allPatientsUuids, allConceptsUuids);
                    if(newPatientsUuids.size()!=0) {
                        observations = observationService.downloadObservations(newPatientsUuids, allConceptsUuids, null);
                        observations.addAll(observationService.downloadObservations(knownPatientsUuid, newConceptsUuids, null));
                        observations.addAll(observationService.downloadObservations(knownPatientsUuid, knownConceptsUuid, fullLastSyncTimeInfo.getLastSyncDate()));
                    }
                    else{
                        observations.addAll(observationService.downloadObservations(patientUuids, conceptUuids, null));
                    }
                }
            }
            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_OBSERVATIONS, sntpService.getLocalTime(), paramSignature);
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
            return observations;
        } catch (IOException e) {
            throw new DownloadObservationException(e);
        }
    }

    private ArrayList<String> getAllUuids(List<String> knownUuids, List<String> newUuids) {
        HashSet<String> allUuids = new HashSet<String>(knownUuids);
        allUuids.addAll(newUuids);
        ArrayList<String> sortedUuids = new ArrayList<String>(allUuids);
        Collections.sort(sortedUuids);
        return sortedUuids;
    }

    private List<String> getNewUuids(List<String> patientUuids, List<String> knownPatientsUuid) {
        List<String> newPatientsUuids = new ArrayList<String>();
        newPatientsUuids.addAll(patientUuids);
        newPatientsUuids.removeAll(knownPatientsUuid);
        return newPatientsUuids;
    }

    private boolean isFirstCallToDownloadObservationsEver(LastSyncTime fullLastSyncTimeInfo) {
        return fullLastSyncTimeInfo == null;
    }

    private boolean hasExactCallBeenMadeBefore(Date lastSyncTime) {
        return lastSyncTime != null;
    }

    private String buildParamSignature(List<String> patientUuids, List<String> conceptUuids) {
        String paramSignature = StringUtils.getCommaSeparatedStringFromList(patientUuids);
        paramSignature += UUID_TYPE_SEPARATOR;
        paramSignature += StringUtils.getCommaSeparatedStringFromList(conceptUuids);
        return paramSignature;
    }

    public void saveObservations(List<Observation> observations) throws SaveObservationException {
        try {
            observationService.saveObservations(observations);
        } catch (IOException e) {
            throw new SaveObservationException(e);
        }
    }

    public void deleteObservations(List<Observation> observations) throws DeleteObservationException {
        try {
            observationService.deleteObservations(observations);
        } catch (IOException e) {
            throw new DeleteObservationException(e);
        }
    }

    public void deleteAllObservations(List<Concept> concepts) throws DeleteObservationException {
        try {
            observationService.deleteObservations(getObservations(concepts));
        } catch (IOException e) {
            throw new DeleteObservationException(e);
        }

    }

    public List<Observation> getObservations(List<Concept> concepts) throws IOException {
        ArrayList<Observation> observations = new ArrayList<Observation>();
        for (Concept concept : concepts) {
            observations.addAll(observationService.getObservations(concept));
        }
        return observations;
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

    public static class DeleteObservationException extends Throwable {
        public DeleteObservationException(Throwable e) {
            super(e);
        }
    }
    public static class ParseObservationException extends Throwable {
        public ParseObservationException(Throwable e) {
            super(e);
        }
        public ParseObservationException(String message) {
            super(message);
        }
    }
}
