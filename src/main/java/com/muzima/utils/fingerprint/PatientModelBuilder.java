package com.muzima.utils.fingerprint;

import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAttribute;
import com.muzima.biometric.model.PatientModel;
import com.muzima.biometric.model.PatientModels;
import com.muzima.utils.Constants;
import com.muzima.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PatientModelBuilder {
    public PatientModels build(List<Patient> patients) {
        ArrayList<PatientModel> models = new ArrayList<PatientModel>();
        for (Patient patient : patients) {
            String fingerPrintTemplate = extractFingerPrintValue(patient);
            if (fingerPrintTemplate != null) {
                PatientModel patientModel = new PatientModel(patient.getUuid(), fingerPrintTemplate);
                models.add(patientModel);
            }
        }
        return new PatientModels(models);
    }

    private String extractFingerPrintValue(Patient patient) {
        PersonAttribute attribute= patient.getAttribute(Constants.FINGER_PRINT_PATIENT_ATTRIBUTE);
        if(attribute == null)
            return null;
        String fingerPrintTemplateString = attribute.getAttribute();
        if (StringUtils.isEmpty(fingerPrintTemplateString))
            return null;
        return fingerPrintTemplateString;
    }
}

