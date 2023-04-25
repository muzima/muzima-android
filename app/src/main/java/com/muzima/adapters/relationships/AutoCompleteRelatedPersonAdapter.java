/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.relationships;

import android.content.Context;
import android.util.Log;
import android.widget.AutoCompleteTextView;

import com.muzima.R;
import com.muzima.adapters.concept.AutoCompleteBaseAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.controller.PatientController;
import com.muzima.controller.PersonController;
import com.muzima.domain.Credentials;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.relationship.RelationshipsListActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AutoCompleteRelatedPersonAdapter extends AutoCompleteBaseAdapter<Person> {

    private boolean searchRemote;
    private boolean connectivityFailed = false;
    private RelationshipsListActivity relationshipsListActivity;
    private Context context;

    public AutoCompleteRelatedPersonAdapter(Context context, int textViewResourceId, AutoCompleteTextView autoCompleteProviderTextView) {
        super(context, textViewResourceId, autoCompleteProviderTextView);
        this.relationshipsListActivity = (RelationshipsListActivity) context;
        this.context = context;
    }

    @Override
    protected List<Person> getOptions(CharSequence constraint) {
        PatientController patientController = getMuzimaApplicationContext().getPatientController();
        PersonController personController = getMuzimaApplicationContext().getPersonController();
        List<Person> personList = new ArrayList<>();
        try {

            List<Patient> patientList = new ArrayList<>();
            if (searchRemote) {
                Credentials credentials = new Credentials(getContext());
                Constants.SERVER_CONNECTIVITY_STATUS serverStatus = NetworkUtils.getServerStatus(getMuzimaApplicationContext(), credentials.getServerUrl());

                if(serverStatus == Constants.SERVER_CONNECTIVITY_STATUS.SERVER_ONLINE)
                    patientList = patientController.searchPatientOnServer(constraint.toString());
                else
                    connectivityFailed = true;
            } else {
                personList = personController.searchPersonLocally(constraint.toString());
                patientList = patientController.searchPatientLocally(constraint.toString(), null);
            }

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
        return person.getDisplayName()+", "+ person.getGender()+", "+context.getString(R.string.general_years, String.format(Locale.getDefault(), "%d ", DateUtils.calculateAge(person.getBirthdate())));
    }

    public void setSearchRemote(boolean searchRemote) {
        this.searchRemote = searchRemote;
        clearPreviousResult();
    }

    public boolean getSearchRemote() {
        return this.searchRemote;
    }

    @Override
    protected void filterComplete(int count) {
        relationshipsListActivity.onFilterComplete(count, connectivityFailed);
    }

    @Override
    protected void clearPreviousResult() {
        super.clearPreviousResult();
    }
}
