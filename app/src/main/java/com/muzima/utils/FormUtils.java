/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Concept;
import com.muzima.api.model.ConceptType;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.EncounterType;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.model.collections.AvailableForms;

import org.json.JSONException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class FormUtils {
    public static AvailableForms getRegistrationForms(FormController formController) {
        AvailableForms availableForms = null;
        try {
            availableForms = formController.getDownloadedRegistrationForms();
        } catch (FormController.FormFetchException e) {
            e.printStackTrace();
        }
        return availableForms;
    }

    public static void handleSaveIndividualObsData(Context context, Patient patient, Date encounterDate, Concept concept, String valueText) {
        boolean conceptIsDouble = false;
        if (concept.isNumeric()) {
            try {
                Double.valueOf(valueText);
                conceptIsDouble = true;
            } catch (NumberFormatException e) {
                conceptIsDouble = false;
            }
        }
        if ((!concept.isCoded() && !concept.isDatetime()) && valueText.isEmpty()) {
            Toast.makeText(context, concept.getName() + " Observation value is needed ", Toast.LENGTH_SHORT).show();
        } else if (concept.isNumeric() && !conceptIsDouble) {
            Toast.makeText(context, concept.getName() + " must be a number ", Toast.LENGTH_SHORT).show();
        } else {
            Observation newObs = constructObservation(concept, valueText, encounterDate, patient);

            if (newObs != null && newObs.getConcept().getUuid() != null) {
                try {
                    List<Observation> obsList = new ArrayList<>();
                    obsList.add(newObs);
                    ((MuzimaApplication) context.getApplicationContext()).getObservationController().saveObservations(obsList);

                    IndividualObsJsonMapper individualObsJsonMapper = new IndividualObsJsonMapper(newObs, patient, ((MuzimaApplication) context.getApplicationContext()));
                    try {
                        FormData formData = individualObsJsonMapper.createFormDataFromObservation();
                        ((MuzimaApplication) context.getApplicationContext()).getFormController().saveFormData(formData);
                    } catch (JSONException e) {
                        Log.e("", "Error While Parsing data" + e);
                    } catch (FormController.FormDataSaveException e) {
                        Log.e("", "Error While Saving Form Data" + e);
                    }

                    Toast.makeText(context, concept.getName() + " observation saved successfully.", Toast.LENGTH_SHORT).show();
                } catch (ObservationController.SaveObservationException e) {
                    Log.e("", "Error While Saving Observations");
                }

            } else {
                Toast.makeText(context, "Unfortunately, mUzima could not save observation.", Toast.LENGTH_LONG).show();
            }
        }

    }

    public static Observation constructObservation(Concept concept, String valueText, Date encounterDateTime, Patient patient) {
        Observation newObservation = new Observation();
        Encounter encounter = new Encounter();
        EncounterType encounterType = getDummyEncounterType();

        encounter.setEncounterDatetime(encounterDateTime);
        encounter.setEncounterType(encounterType);
        encounter.setUuid(UUID.randomUUID().toString());

        newObservation.setConcept(concept);
        newObservation.setUuid(UUID.randomUUID().toString() + new Random().nextInt());
        newObservation.setEncounter(encounter);
        newObservation.setValueCoded(defaultValueCodedConcept());
        newObservation.setPerson(patient);
        newObservation.setObservationDatetime(encounterDateTime);
        newObservation.setValueText(valueText);

        if (concept.isNumeric()) {
            newObservation.setValueNumeric(Double.valueOf(valueText));
        }

        if (concept.isDatetime()) {
            newObservation.setValueDatetime(parseDateTime(valueText));
        }

        if (concept.isCoded()) {
            //TODO Enable coded individual obs after fixing MUZIMA-620
        }

        if (!concept.isNumeric() && !concept.isDatetime() && !concept.isCoded()) {
            newObservation.setValueText(valueText);
        }

        return newObservation;
    }

    public static EncounterType getDummyEncounterType() {
        EncounterType encounterType = new EncounterType();
        encounterType.setUuid("encounterTypeForObservationsCreatedOnPhone");
        encounterType.setName("encounterTypeForObservationsCreatedOnPhone");
        return encounterType;
    }

    public static Date parseDateTime(String dateTime) {
        try {
            DateFormat dateTimeFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            if (dateTime.length() <= 10) {
                dateTime = dateTime.concat(" 00:00");
            }
            return dateTimeFormat.parse(dateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Concept defaultValueCodedConcept() {
        Concept valueCoded = new Concept();
        valueCoded.setConceptType(new ConceptType());
        return valueCoded;
    }
}
