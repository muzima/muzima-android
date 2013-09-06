package com.muzima.adapters.forms;

import android.content.Context;

import com.muzima.api.model.Form;
import com.muzima.controller.FormController;

import java.util.List;

public class AllCompleteFormsAdapter extends IncompleteFormsAdapter {
    private static final String TAG = "AllCompleteFormsAdapter";

    public AllCompleteFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
    }

    @Override
    protected List<Form> fetchForms() throws FormController.FormFetchException {
        return formController.getAllCompleteForms();
    }
}
