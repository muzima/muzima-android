package com.muzima.adapters.forms;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.muzima.api.model.Form;
import com.muzima.controller.FormController;
import com.muzima.tasks.QueryTask;

import java.util.List;

public class AllIncompleteFormsAdapter extends IncompleteFormsAdapter {
    private static final String TAG = "AllIncompleteFormsAdapter";

    public AllIncompleteFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
    }

    @Override
    protected List<Form> fetchForms() throws FormController.FormFetchException {
        return formController.getAllIncompleteForms();
    }
}
