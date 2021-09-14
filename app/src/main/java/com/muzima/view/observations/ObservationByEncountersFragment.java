/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.observations;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.observations.ObservationsByEncounterAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;

public class ObservationByEncountersFragment extends ObservationsListFragment{

    private Boolean isShrEncounter = false;
    private Patient patient;
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    public static ObservationByEncountersFragment newInstance(EncounterController encounterController,
                                                              ObservationController observationController,
                                                              Boolean isShrEncounter, Patient patient) {
        ObservationByEncountersFragment f = new ObservationByEncountersFragment();
        f.observationController = observationController;
        f.encounterController = encounterController;
        f.isShrEncounter = isShrEncounter;
        f.patient = patient;

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(listAdapter == null){
            listAdapter = new ObservationsByEncounterAdapter(
                    getActivity(), R.layout.item_observation_by_encounter_list,encounterController, conceptController, observationController,isShrEncounter);
        }
        noDataMsg = getActivity().getResources().getString(R.string.info_observation_in_progress);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser && isResumed()){
            logEvent("VIEW_CLIENT_OBS_BY_DATE","{\"patientuuid\":\""+patient.getUuid()+"\"}");
        }
    }

    @Override
    public void onSearchTextChange(String query) {
        ((ObservationsByEncounterAdapter)listAdapter).search(query);
    }

    @Override
    public void onQueryTaskCancelled(){
        ObservationsByEncounterAdapter observationsByEncounterAdapter = ((ObservationsByEncounterAdapter)listAdapter);
        observationsByEncounterAdapter.cancelBackgroundQueryTask();
    }

    @Override
    public void onQueryTaskCancelled(Object errorDefinition){
        onQueryTaskCancelled();
    }
}
