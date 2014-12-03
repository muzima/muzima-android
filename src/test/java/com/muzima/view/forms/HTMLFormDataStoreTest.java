/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import com.muzima.api.model.FormData;
import com.muzima.controller.FormController;
import com.muzima.service.HTMLFormObservationCreator;
import com.muzima.testSupport.CustomTestRunner;
import com.muzima.utils.Constants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(CustomTestRunner.class)
public class HTMLFormDataStoreTest {

    private FormController formController;
    private HTMLFormWebViewActivity formWebViewActivity;
    private FormData formData;
    private HTMLFormObservationCreator htmlFormObservationCreator;
    private HTMLFormDataStore htmlFormDataStore;

    @Before
    public void setUp() throws Exception {
        formController = mock(FormController.class);
        formWebViewActivity = mock(HTMLFormWebViewActivity.class);
        formData = mock(FormData.class);
        htmlFormObservationCreator = mock(HTMLFormObservationCreator.class);
        htmlFormDataStore = new HTMLFormDataStore(formWebViewActivity, formController, formData){
            @Override
            public HTMLFormObservationCreator getFormParser(){
                return htmlFormObservationCreator;
            }
        };
    }

    @Test
    public void shouldParsedPayloadForCompletedForm() {
        String jsonPayLoad = "{'patient':{'patient.uuid':'10aa0fa5-48fd-4987-98e9-54c01d09dd52','patient.family_name':'rr','patient.given_name':'rt','patient.sex':'M','patient.medical_record_number':'2082KT-0','patient.finger_print':'fingerprint data 123','patient.birth_date':'01-12-2014','patient.birthdate_estimated':'true'},'tmp':{'tmp.birthdate_type':'birthdate'},'encounter':{'encounter.location_id':'3','encounter.provider_id':'1','encounter.encounter_datetime':'02-12-2014'}}";
        htmlFormDataStore.saveHTML(jsonPayLoad, Constants.STATUS_COMPLETE);
        verify(htmlFormObservationCreator).createAndPersistObservations(jsonPayLoad,formData.getUuid());
    }

    @Test
    public void shouldNotParseIncompletedForm() {
        String jsonPayLoad = "jsonPayLoad";
        htmlFormDataStore.saveHTML(jsonPayLoad, Constants.STATUS_INCOMPLETE);
        verify(htmlFormObservationCreator, times(0)).createAndPersistObservations(jsonPayLoad,formData.getUuid());
    }
}
