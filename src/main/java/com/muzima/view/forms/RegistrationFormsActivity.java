package com.muzima.view.forms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.RegistrationFormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.collections.AvailableForms;
import com.muzima.view.BaseActivity;

public class RegistrationFormsActivity extends BaseActivity {
    private ListView list;
    private RegistrationFormsAdapter registrationFormsAdapter;
    private String TAG = "RegistrationFormsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_form_list);

        FormController formController = ((MuzimaApplication) getApplicationContext()).getFormController();
        AvailableForms availableForms = getRegistrationForms(formController);
        if (isOnlyOneRegistrationForm(availableForms)) {
            startBarcodeActivity(availableForms.get(0));
        } else {
            prepareRegistrationAdapter(formController, availableForms);
        }
    }

    private void prepareRegistrationAdapter(FormController formController, AvailableForms availableForms) {
        registrationFormsAdapter = new RegistrationFormsAdapter(getApplicationContext(), R.layout.item_forms_list,
                formController, availableForms);
        list = (ListView) findViewById(R.id.list);
        list.setOnItemClickListener(startRegistrationOnClick());
        list.setAdapter(registrationFormsAdapter);
        registrationFormsAdapter.reloadData();
    }

    private boolean isOnlyOneRegistrationForm(AvailableForms availableForms) {
        return availableForms.size() == 1;
    }

    private AdapterView.OnItemClickListener startRegistrationOnClick() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AvailableForm form = registrationFormsAdapter.getItem(position);
                startBarcodeActivity(form);
            }
        };
    }

    private void startBarcodeActivity(AvailableForm form) {
        Intent intent = new Intent(this, RegistrationBarcodeActivity.class);
        intent.putExtra(RegistrationBarcodeActivity.SELECTED_REG_FORM, form);
        startActivity(intent);
    }

    private AvailableForms getRegistrationForms(FormController formController) {
        AvailableForms availableForms = null;
        try {
            availableForms = formController.getDownloadedRegistrationForms();
        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Error while retireving registration forms from Lucene");
        }
        return availableForms;
    }
}
