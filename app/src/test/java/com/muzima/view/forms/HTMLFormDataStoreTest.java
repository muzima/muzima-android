/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.FormData;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.ProviderController;
import com.muzima.service.HTMLFormObservationCreator;
import com.muzima.utils.Constants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.util.Scanner;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class HTMLFormDataStoreTest {

    private HTMLFormWebViewActivity formWebViewActivity;
    private FormData formData;
    private HTMLFormObservationCreator htmlFormObservationCreator;
    private HTMLFormDataStore htmlFormDataStore;

    @Mock
    private ConceptController conceptController;
    @Mock
    private MuzimaSettingController settingController;
    @Mock
    private ObservationController observationController;
    @Mock
    private EncounterController encounterController;

    @Before
    public void setUp() {
        FormController formController = mock(FormController.class);
        LocationController locationController = mock(LocationController.class);
        ProviderController providerController = mock(ProviderController.class);
        formWebViewActivity = mock(HTMLFormWebViewActivity.class);
        formData = mock(FormData.class);
        htmlFormObservationCreator = mock(HTMLFormObservationCreator.class);
        MuzimaApplication muzimaApplication = mock(MuzimaApplication.class);

        when(muzimaApplication.getFormController()).thenReturn(formController);
        when(muzimaApplication.getConceptController()).thenReturn(conceptController);
        when(muzimaApplication.getProviderController()).thenReturn(providerController);
        when(muzimaApplication.getLocationController()).thenReturn(locationController);
        when(muzimaApplication.getProviderController()).thenReturn(providerController);
        when(muzimaApplication.getObservationController()).thenReturn(observationController);
        when(muzimaApplication.getEncounterController()).thenReturn(encounterController);
        when(muzimaApplication.getMuzimaSettingController()).thenReturn(settingController);
        htmlFormDataStore = new HTMLFormDataStore(formWebViewActivity, formData, muzimaApplication){
            @Override
            public HTMLFormObservationCreator getFormParser(){
                return htmlFormObservationCreator;
            }
        };
    }

//    @Test
//    public void shouldParsedPayloadForCompletedForm() {
//        when(formWebViewActivity.getString(anyInt())).thenReturn("success");
//        when(formController.isRegistrationFormData(formData)).thenReturn(false);
//        String jsonPayLoad = readFile();
//
//        htmlFormDataStore.saveHTML(jsonPayLoad, Constants.STATUS_COMPLETE);
//        verify(htmlFormObservationCreator).createAndPersistObservations(jsonPayLoad,formData.getUuid());
//    }

    @Test
    public void shouldNotParseIncompletedForm() {
        when(formWebViewActivity.getString(anyInt())).thenReturn("success");
        String jsonPayLoad = readFile();
        htmlFormDataStore.saveHTML(jsonPayLoad, Constants.STATUS_INCOMPLETE);
        verify(htmlFormObservationCreator, times(0)).createAndPersistObservations(jsonPayLoad,formData.getUuid());
    }

    public String readFile() {
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("html/dispensary.json");
        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "{}";
    }
}
