/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.service.FormParser;
import com.muzima.testSupport.CustomTestRunner;
import com.muzima.utils.Constants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;

import static com.muzima.utils.Constants.FORM_DISCRIMINATOR_REGISTRATION;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(CustomTestRunner.class)
public class FormDataStoreTest {

    private FormController controller;
    private FormWebViewActivity activity;
    private FormData formData;
    private FormDataStore store;

    @Mock
    private FormParser formParser;
    private MuzimaApplication muzimaApplication;
    private ObservationController obsController;


    @Before
    public void setUp() throws Exception {
        initMocks(this);
        controller = mock(FormController.class);
        activity = mock(FormWebViewActivity.class);
        formData = new FormData();
        formData.setPatientUuid("adasdssd");
        muzimaApplication = mock(MuzimaApplication.class);
        obsController = mock(ObservationController.class);
        when(muzimaApplication.getObservationController()).thenReturn(obsController);
        when(activity.getApplicationContext()).thenReturn(muzimaApplication);
        store = new FormDataStore(activity, controller, formData){
            @Override
            public FormParser getFormParser() {
                return formParser;
            }
        };
    }

    @Test
    public void save_shouldSaveFormDataWithStatus() throws Exception, FormController.FormDataSaveException {
        store.save("data", "xmldata", "status");
        verify(controller).saveFormData(formData);
        verify(activity).finish();
        assertThat(formData.getJsonPayload(), is("data"));
        assertThat(formData.getStatus(), is("status"));
    }

    @Test
    public void save_shouldNotFinishTheActivityIfThereIsAnExceptionWhileSaving() throws Exception, FormController.FormDataSaveException {
        doThrow(new FormController.FormDataSaveException(null)).when(controller).saveFormData(formData);
        store.save("data", "xmldata", "status");
        verify(activity, times(0)).finish();
    }

    @Test
    public void getFormPayload_shouldGetTheFormDataPayload() throws Exception {
        formData.setJsonPayload("payload");
        assertThat(store.getFormPayload(), is("payload"));
    }

    @Test
    public void shouldCreateANewPatientAndStoreHisUUIDAsPatientUUID() throws Exception {
        String tempUUIDAssignedByDevice = "newUUID";
        formData.setPatientUuid(null);
        formData.setDiscriminator(FORM_DISCRIMINATOR_REGISTRATION);
        Patient patient = new Patient();
        patient.setUuid(tempUUIDAssignedByDevice);
        when(controller.createNewPatient("data")).thenReturn(patient);
        store.save("data", "xmlData", "complete");
        assertThat(formData.getXmlPayload(), is("xmlData"));
        assertThat(formData.getPatientUuid(), is(tempUUIDAssignedByDevice));
    }

    @Test
    public void shouldParseObservationsInProvidedPayloadWhenSavingAsFinal() throws ConceptController.ConceptSaveException,
            ParseException, XmlPullParserException, PatientController.PatientLoadException,ConceptController.ConceptFetchException,
            ConceptController.ConceptParseException, IOException, ObservationController.ParseObservationException {
        String xmlPayload = "xmldata";
        store.save("data", xmlPayload, Constants.STATUS_COMPLETE);

        verify(formParser).parseAndSaveObservations(xmlPayload,formData.getUuid());
    }

    @Test
    public void shouldNotParseObservationsForIncompleteForm() throws ConceptController.ConceptSaveException, ParseException,
            XmlPullParserException, PatientController.PatientLoadException, ConceptController.ConceptFetchException,
            ConceptController.ConceptParseException, ObservationController.ParseObservationException, IOException {
        store.save("data", "xmldata", Constants.STATUS_INCOMPLETE);

        verify(formParser, times(0)).parseAndSaveObservations(anyString(),anyString());
    }
}
