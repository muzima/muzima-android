package com.muzima.view.observations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.adapters.observations.ObservationsByDateAdapter;
import com.muzima.controller.PatientController;
import com.muzima.view.patients.ObservationsListFragment;

public class ObservationsByDateListFragment extends ObservationsListFragment {

    public static ObservationsByDateListFragment newInstance(PatientController patientController) {
        ObservationsByDateListFragment f = new ObservationsByDateListFragment();
        f.patientController = patientController;
        f.setRetainInstance(true);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(listAdapter == null){
            listAdapter = new ObservationsByDateAdapter(getActivity(), R.layout.item_observation_list, patientController);
        }
        noDataMsg = getActivity().getResources().getString(R.string.no_observations_available);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.layout_synced_list, container, false);
        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }
}
