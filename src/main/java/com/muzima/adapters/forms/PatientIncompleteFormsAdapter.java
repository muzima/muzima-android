package com.muzima.adapters.forms;

import android.content.Context;

import com.muzima.api.model.Form;
import com.muzima.controller.FormController;

import java.util.List;

public class PatientIncompleteFormsAdapter extends IncompleteFormsAdapter {
    private static final String TAG = "AllIncompleteFormsAdapter";
    private final String patientId;

    public PatientIncompleteFormsAdapter(Context context, int textViewResourceId, FormController formController, String patientId) {
        super(context, textViewResourceId, formController);
        this.patientId = patientId;
    }

    @Override
    protected List<Form> fetchForms() throws FormController.FormFetchException {
        return formController.getAllIncompleteFormsForPatientUuid(patientId);
    }
}
