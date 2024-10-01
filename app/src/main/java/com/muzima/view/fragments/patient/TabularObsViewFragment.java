/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.fragments.patient;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.controller.ConceptController;
import com.muzima.controller.DerivedConceptController;
import com.muzima.controller.DerivedObservationController;
import com.muzima.view.custom.TableFixHeaders;
import com.muzima.adapters.observations.BaseTableAdapter;
import com.muzima.adapters.observations.ObservationGroupAdapter;

import java.io.IOException;

public class TabularObsViewFragment extends Fragment implements RecyclerAdapter.BackgroundListQueryTaskListener {
    private String patientUuid;
    private Context context;

    public TabularObsViewFragment() {

    }

    public static TabularObsViewFragment newInstance(String patientUuid, Context context) {
        TabularObsViewFragment f = new TabularObsViewFragment();
        f.patientUuid = patientUuid;
        f.context = context;
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.table, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        int obsCount = 0;
        int conceptCount = 0;
        try {
            obsCount = ((MuzimaApplication) context.getApplicationContext()).getObservationController().getObservationsCountByPatient(patientUuid);
            obsCount+= ((MuzimaApplication) context.getApplicationContext()).getDerivedObservationController().getDerivedObservationByPatientUuid(patientUuid).size();
            conceptCount = ((MuzimaApplication) context.getApplicationContext()).getConceptController().getConcepts().size();
            conceptCount+= ((MuzimaApplication) context.getApplicationContext()).getDerivedConceptController().getDerivedConcepts().size();
        } catch (IOException | ConceptController.ConceptFetchException e) {
            Log.e(getClass().getSimpleName(),"Exception encountered while loading Observations ",e);
        } catch (DerivedObservationController.DerivedObservationFetchException e) {
            Log.e(getClass().getSimpleName(),"Exception encountered while loading Derived Obs ",e);
        } catch (DerivedConceptController.DerivedConceptFetchException e) {
            Log.e(getClass().getSimpleName(),"Exception encountered while loading Derived concepts ",e);
        }
        if (obsCount == 0) {
            view.findViewById(R.id.no_data_layout).setVisibility(View.VISIBLE);
            TextView noDataTip = view.findViewById(R.id.no_data_tip);
            if(conceptCount == 0){
                noDataTip.setText(R.string.info_no_observation_and_concept_data_tip);
            }else{
                noDataTip.setText(R.string.info_no_observation_for_concept_data_tip);
            }
            view.findViewById(R.id.table).setVisibility(View.GONE);
        }

        TableFixHeaders tableFixHeaders = view.findViewById(R.id.table);
        BaseTableAdapter baseTableAdapter = new ObservationGroupAdapter(context, patientUuid);
        tableFixHeaders.setAdapter(baseTableAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onQueryTaskStarted() {}

    @Override
    public void onQueryTaskFinish() {}

    @Override
    public void onQueryTaskCancelled() {}

    @Override
    public void onQueryTaskCancelled(Object errorDefinition) {}

}