package com.muzima.service;

import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FormParserTest {

    private FormParser formParser;

    @Mock
    private PatientController patientController;

    @Mock
    private ObservationController observationController;

    @Mock
    private ConceptController conceptController;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldCreateObservations() throws IOException, XmlPullParserException, ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException {
        String xml = readFile();
        formParser = new FormParser(xml, new MXParser(), patientController, conceptController, observationController);
        Patient patient = new Patient();
        when(patientController.getPatientByUuid("dd7963a8-1691-11df-97a5-7038c432aabf")).thenReturn(patient);
        List<Observation> observations = formParser.parseForm();
        assertEquals(6, observations.size());
    }

    public String readFile() {
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("xml/histo_xml_payload.xml");
        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "{}";
    }
}
