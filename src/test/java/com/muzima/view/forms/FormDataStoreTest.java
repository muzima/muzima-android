package com.muzima.view.forms;

import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.testSupport.CustomTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import static com.muzima.utils.Constants.FORM_DISCRIMINATOR_REGISTRATION;

@RunWith(CustomTestRunner.class)
public class FormDataStoreTest {

    private FormController controller;
    private FormWebViewActivity activity;
    private FormData formData;
    private FormDataStore store;

    @Before
    public void setUp() throws Exception {
        controller = mock(FormController.class);
        activity = mock(FormWebViewActivity.class);
        formData = new FormData();
        formData.setPatientUuid("adasdssd");
        store = new FormDataStore(activity, controller, formData);
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
}
