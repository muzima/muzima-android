package com.muzima.utils;

import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Location;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PatientIdentifierType;
import com.muzima.controller.LocationController;
import com.muzima.controller.PatientController;

import java.util.List;
import java.util.UUID;

import static com.muzima.utils.Constants.FORM_JSON_DISCRIMINATOR_SHR_DEMOGRAPHICS_UPDATE;
import static com.muzima.utils.Constants.FORM_JSON_DISCRIMINATOR_SHR_REGISTRATION;
import static com.muzima.utils.Constants.STATUS_COMPLETE;

public class PatientIdentifierUtils {
    public static PatientIdentifier getOrCreateKenyaEmrIdentifier(MuzimaApplication muzimaApplication, String identifierValue, String shrIdentifierTypeName,
                                                                  String assigningFacility) throws Exception {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        patientIdentifier.setIdentifier(identifierValue);

        String identifierTypeName = null;
        String identifierTypeUuid = null;
        switch (shrIdentifierTypeName) {
            case Constants.Shr.KenyaEmr.PersonIdentifierType.CARD_SERIAL_NUMBER.shr_name:
                identifierTypeName = Constants.Shr.KenyaEmr.PersonIdentifierType.CARD_SERIAL_NUMBER.name;
                identifierTypeUuid = Constants.Shr.KenyaEmr.PersonIdentifierType.CARD_SERIAL_NUMBER.uuid;
                break;
            case Constants.Shr.KenyaEmr.PersonIdentifierType.CCC_NUMBER.shr_name:
                identifierTypeName = Constants.Shr.KenyaEmr.PersonIdentifierType.CCC_NUMBER.name;
                identifierTypeUuid = Constants.Shr.KenyaEmr.PersonIdentifierType.CCC_NUMBER.uuid;
                break;
            case Constants.Shr.KenyaEmr.PersonIdentifierType.GODS_NUMBER.shr_name:
                identifierTypeName = Constants.Shr.KenyaEmr.PersonIdentifierType.GODS_NUMBER.name;
                identifierTypeUuid = Constants.Shr.KenyaEmr.PersonIdentifierType.GODS_NUMBER.uuid;
                break;
            case Constants.Shr.KenyaEmr.PersonIdentifierType.HEI_NUMBER.shr_name:
                identifierTypeName = Constants.Shr.KenyaEmr.PersonIdentifierType.HEI_NUMBER.name;
                identifierTypeUuid = Constants.Shr.KenyaEmr.PersonIdentifierType.HEI_NUMBER.uuid;
                break;
            case Constants.Shr.KenyaEmr.PersonIdentifierType.HTS_NUMBER.shr_name:
                identifierTypeName = Constants.Shr.KenyaEmr.PersonIdentifierType.HTS_NUMBER.name;
                identifierTypeUuid = Constants.Shr.KenyaEmr.PersonIdentifierType.HTS_NUMBER.uuid;
                break;
            case Constants.Shr.KenyaEmr.PersonIdentifierType.NATIONAL_ID.shr_name:
                identifierTypeName = Constants.Shr.KenyaEmr.PersonIdentifierType.NATIONAL_ID.name;
                identifierTypeUuid = Constants.Shr.KenyaEmr.PersonIdentifierType.NATIONAL_ID.uuid;
                break;
            case Constants.Shr.KenyaEmr.CONCEPTS.ANC_NUMBER.shr_name:
                identifierTypeName = Constants.Shr.KenyaEmr.CONCEPTS.ANC_NUMBER.name;
                identifierTypeUuid = Constants.Shr.KenyaEmr.CONCEPTS.ANC_NUMBER.uuid;
                break;
        }

        if (!StringUtils.isEmpty(identifierTypeName) && !StringUtils.isEmpty(identifierTypeUuid)){
            PatientIdentifierType identifierType = getOrCreateDummyPatientIdentifierType(muzimaApplication ,identifierTypeName, identifierTypeUuid);
            patientIdentifier.setIdentifierType(identifierType);
        } else {
            throw new Exception("Cannot create identifier. Could not determine identifier type name or uuid");
        }

        Location location = LocationUtils.getOrCreateDummyLocationByKenyaEmrMasterFacilityListCode(muzimaApplication,assigningFacility);
        patientIdentifier.setLocation(location);

        return patientIdentifier;
    }

    public static PatientIdentifierType getOrCreateDummyPatientIdentifierType(MuzimaApplication muzimaApplication, String identifierTypeName, String identifierTypeUuid){
        PatientIdentifierType identifierType = null;
        List<PatientIdentifierType> identifierTypes = null;

        PatientController patientController = muzimaApplication.getPatientController();

        if(!StringUtils.isEmpty(identifierTypeName)){
            identifierTypes = patientController.getPatientIdentifierTypeByName(identifierTypeName);

        }
        if(identifierType == null && !StringUtils.isEmpty(identifierTypeUuid)){
            identifierTypes = patientController.getPatientIdentifierTypeByName(identifierTypeName);
        }

        if(!identifierTypes.isEmpty()){
            identifierType = identifierTypes.get(0);
        }

        if(identifierType == null){
            //ToDo: Figure out how to get identifierType from server side
            identifierType = new PatientIdentifierType();
            identifierType.setName(identifierTypeName);
            identifierType.setUuid(identifierTypeUuid);
        }
        return identifierType;
    }
}
