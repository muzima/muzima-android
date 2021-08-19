package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;

import java.util.List;

public class LoadPatientsListService implements Runnable {

    private Context context;
    private PatientsListLoadedCallback callback;

    public LoadPatientsListService(Context context, PatientsListLoadedCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            List<Patient> patientList = ((MuzimaApplication) context.getApplicationContext()).getPatientController().getAllPatients();
            callback.onPatientsLoaded(patientList);
        } catch (PatientController.PatientLoadException e) {
            e.printStackTrace();
        }
    }

    public interface PatientsListLoadedCallback {
        void onPatientsLoaded(List<Patient> patients);
    }
}
