/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.patients;

import android.content.Context;

import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SimilarPatientsLocalSearchAdapter extends PatientsLocalSearchAdapter implements MuzimaAsyncTask.OnProgressListener {
    private Patient comparisonPatient;


    public SimilarPatientsLocalSearchAdapter(Context context, PatientController patientController, Patient comparisonPatient) {
        super(context,patientController,null,null,null,null, null);
        this.comparisonPatient = comparisonPatient;
        setShowAdditionalDetails(true);
    }

    @Override
    protected void onPostExecuteUpdate(List<Patient> patients) {
        if (patients != null) {
            List<Patient> similarPatients = new ArrayList<>();
            if(comparisonPatient.getBirthdate() != null) {
                int comparisonYear = comparisonPatient.getBirthdate().getYear();
                String comparisonGender = comparisonPatient.getGender();
                for (Patient patient : patients) {
                    boolean isSimilar = patient.getBirthdate()!= null && patient.getBirthdate().getYear() == comparisonYear
                            && StringUtils.equals(patient.getGender(), comparisonGender);
                    if (isSimilar) {
                        similarPatients.add(patient);
                    }
                }
            }
            patients = similarPatients;
        }
        super.onPostExecuteUpdate(patients);
    }

}
