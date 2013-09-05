package com.muzima.view.forms;

import com.muzima.api.model.FormData;
import com.muzima.controller.FormController;
import com.muzima.testSupport.CustomTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
        store = new FormDataStore(activity, controller, formData);
    }

    @Test
    public void save_shouldSaveFormDataWithStatus() throws Exception, FormController.FormDataSaveException {
        store.save("data", "status");
        verify(controller).saveFormData(formData);
        verify(activity).finish();
        assertThat(formData.getPayload(), is("data"));
        assertThat(formData.getStatus(), is("status"));
    }

    @Test
    public void save_shouldNotFinishTheActivityIfThereIsAnExceptionWhileSaving() throws Exception, FormController.FormDataSaveException {
        doThrow(new FormController.FormDataSaveException(null)).when(controller).saveFormData(formData);
        store.save("data", "status");
        verify(activity, times(0)).finish();
    }

    @Test
    public void getFormPayload_shouldGetTheFormDataPayload() throws Exception {
        formData.setPayload("payload");
        assertThat(store.getFormPayload(), is("payload"));
    }
}
