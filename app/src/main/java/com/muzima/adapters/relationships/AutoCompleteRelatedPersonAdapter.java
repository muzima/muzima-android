/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.relationships;

import android.content.Context;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import com.muzima.adapters.concept.AutoCompleteBaseAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.controller.PatientController;
import com.muzima.controller.PersonController;

import java.util.ArrayList;
import java.util.List;

public class AutoCompleteRelatedPersonAdapter extends AutoCompleteBaseAdapter<Person> {

    private boolean searchRemote;

    public AutoCompleteRelatedPersonAdapter(Context context, int textViewResourceId, AutoCompleteTextView autoCompleteProviderTextView) {
        super(context, textViewResourceId, autoCompleteProviderTextView);
    }

    @Override
    protected List<Person> getOptions(CharSequence constraint) {
        PatientController patientController = getMuzimaApplicationContext().getPatientController();
        PersonController personController = getMuzimaApplicationContext().getPersonController();
        List<Person> personList = new ArrayList<>();
        try {
            personList = personController.searchPersonLocally(constraint.toString());

            List<Patient> patientList = patientController.searchPatientLocally(constraint.toString(), null);
            for (Patient patient : patientList) {
                if (personController.getPersonByUuid(patient.getUuid()) == null)
                    personList.add(patient);
            }
        } catch (PersonController.PersonLoadException | PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Unable to search persons!", e);
        }
        return personList;
    }

    @Override
    protected String getOptionName(Person person) {
        return person.getDisplayName();
    }

    public void setSearchRemote(boolean searchRemote) {
        this.searchRemote = searchRemote;
    }
}