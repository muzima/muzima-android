package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.model.CohortFilter;
import com.muzima.model.events.CohortFilterActionEvent;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public class FilterPatientsListTask implements Runnable {

    private Context context;
    private CohortFilterActionEvent event;
    private PatientsListFilterCallback patientsListFilterCallback;

    public FilterPatientsListTask(Context context, CohortFilterActionEvent event, PatientsListFilterCallback patientsListFilterCallback) {
        this.context = context;
        this.event = event;
        this.patientsListFilterCallback = patientsListFilterCallback;
    }

    @Override
    public void run() {
        try {
            List<Patient> patientList = new ArrayList<>();
            List<CohortFilter> filters = event.getFilters();
            if(filters.size() == 0){
                patientList = ((MuzimaApplication) context.getApplicationContext()).getPatientController()
                        .getAllPatients();
            }else {
                for (CohortFilter filter : filters) {
                    if (filter.getCohort() == null && !event.isNoSelectionEvent()) {
                        patientList = ((MuzimaApplication) context.getApplicationContext()).getPatientController()
                                .getAllPatients();
                    } else if (filter.getCohort() != null && !event.isNoSelectionEvent()) {
                        patientList = ((MuzimaApplication) context.getApplicationContext()).getPatientController()
                                .getPatientsForCohorts(new String[]{filter.getCohort().getUuid()});
                    }
                }
            }
            patientsListFilterCallback.onPatientsFiltered(patientList);
        } catch (PatientController.PatientLoadException | ConcurrentModificationException ex) {
            ex.printStackTrace();
        }
    }

    public interface PatientsListFilterCallback {
        void onPatientsFiltered(List<Patient> patientList);
    }
}
