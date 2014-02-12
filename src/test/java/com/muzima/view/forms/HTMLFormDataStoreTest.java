package com.muzima.view.forms;

import com.muzima.api.model.FormData;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.service.HTMLFormObservationCreator;
import com.muzima.testSupport.CustomTestRunner;
import com.muzima.utils.Constants;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.ParseException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(CustomTestRunner.class)
public class HTMLFormDataStoreTest {

    @Test
    public void shouldParsedPayloadForCompletedForm() throws ObservationController.SaveObservationException, EncounterController.SaveEncounterException, ConceptController.ConceptSaveException, ParseException, JSONException {
        FormController formController = mock(FormController.class);
        HTMLFormWebViewActivity formWebViewActivity = mock(HTMLFormWebViewActivity.class);
        FormData formData = mock(FormData.class);
        final HTMLFormObservationCreator htmlFormObservationCreator = mock(HTMLFormObservationCreator.class);
        final HTMLFormDataStore htmlFormDataStore = new HTMLFormDataStore(formWebViewActivity, formController, formData){
            @Override
            public HTMLFormObservationCreator getFormParser(){
                return htmlFormObservationCreator;
            }
        };

        String jsonPayLoad = "jsonPayLoad";
        htmlFormDataStore.saveHTML(jsonPayLoad, Constants.STATUS_COMPLETE);
        verify(htmlFormObservationCreator).createAndPersistObservations(jsonPayLoad);
    }
}
