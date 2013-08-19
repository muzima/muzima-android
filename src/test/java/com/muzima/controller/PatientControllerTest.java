package com.muzima.controller;

import com.muzima.api.model.Patient;
import com.muzima.api.service.PatientService;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PatientControllerTest {

    private PatientController patientController;
    private PatientService patientService;

    @Before
    public void setup(){
        patientService = mock(PatientService.class);
        patientController = new PatientController(patientService);
    }

    @Test
    public void replacePatients_shouldReplaceAllExistingPatientsAndAddNewPatients() throws IOException, PatientController.PatientReplaceException {
        List<Patient> patients = buildPatients();

        when(patientService.getPatientByUuid("uuid1")).thenReturn(null);
        when(patientService.getPatientByUuid("uuid2")).thenReturn(patients.get(1));
        when(patientService.getPatientByUuid("uuid3")).thenReturn(null);

        patientController.replacePatients(patients);

        verify(patientService, times(3)).getPatientByUuid(anyString());
        verify(patientService).deletePatient(patients.get(1));
        verify(patientService).savePatient(patients.get(0));
        verify(patientService).savePatient(patients.get(1));
        verify(patientService).savePatient(patients.get(2));
        verifyNoMoreInteractions(patientService);
    }

    @Test(expected = PatientController.PatientReplaceException.class)
    public void replacePatients_shouldThrowPatientReplaceExceptionIfExceptionThrownByService() throws IOException, PatientController.PatientReplaceException {
        List<Patient> patients = buildPatients();

        doThrow(new IOException()).when(patientService).getPatientByUuid(patients.get(0).getUuid());

        patientController.replacePatients(patients);
    }

    private List<Patient> buildPatients() {
        ArrayList<Patient> patients = new ArrayList<Patient>();
        Patient patient1 = new Patient();
        patient1.setUuid("uuid1");
        Patient patient2 = new Patient();
        patient2.setUuid("uuid2");
        Patient patient3 = new Patient();
        patient3.setUuid("uuid3");
        patients.add(patient1);
        patients.add(patient2);
        patients.add(patient3);
        return patients;
    }
}
