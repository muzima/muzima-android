/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.forms;

import android.content.Context;
import androidx.annotation.NonNull;

import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PersonName;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.model.CompleteFormWithPatientData;
import com.muzima.model.collections.CompleteFormsWithPatientData;
import com.muzima.testSupport.CustomTestRunner;
import com.muzima.utils.StringUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
//import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static com.muzima.adapters.forms.CompleteFormsWithDataAdapter.BackgroundQueryTask;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(CustomTestRunner.class)
@Config(manifest= Config.NONE)
public class CompleteFormsAdapterTest {
    private CompleteFormsWithDataAdapter formsAdapter;
    private FormController formController;
    private ObservationController observationController;
    private Context context;

    @Before
    public void setUp() {
        formController = mock(FormController.class);
        observationController = mock(ObservationController.class);
        context = mock(Context.class);
        formsAdapter = new CompleteFormsWithDataAdapter(context, 0, StringUtils.EMPTY,formController, observationController){
            @Override
            public void addAll(@NonNull Collection<? extends CompleteFormWithPatientData> collection) {

            }
        };

        Robolectric.getBackgroundThreadScheduler().pause();
        Robolectric.getForegroundThreadScheduler().pause();
    }

    @Test
    public void queryTask_shouldFetchAllCompletedForms() throws Exception, FormController.FormFetchException {
        BackgroundQueryTask queryTask = new BackgroundQueryTask(formsAdapter);
        CompleteFormsWithPatientData completeFormsWithPatientData = new CompleteFormsWithPatientData();
        completeFormsWithPatientData.add(new CompleteFormWithPatientData());

        when(formController.getAllCompleteFormsWithPatientData(context,StringUtils.EMPTY)).thenReturn(completeFormsWithPatientData);

        queryTask.execute();
        Robolectric.flushBackgroundThreadScheduler();
    }

    private CompleteFormWithPatientData completeFormWithPatientData(final Patient patient1) {
        return new CompleteFormWithPatientData() {{
            setPatient(patient1);
        }};
    }

    private Patient patient(String identifier) {
        Patient patient = new Patient();
        PersonName personName = new PersonName();
        personName.setFamilyName("familyName");
        personName.setMiddleName("middleName");
        personName.setGivenName("givenName");
        patient.setNames(new ArrayList(Collections.singletonList(personName)));
        PatientIdentifier personId = new PatientIdentifier();
        personId.setIdentifier(identifier);
        patient.setIdentifiers(new ArrayList(Collections.singletonList(personName)));
        return patient;
    }
}
