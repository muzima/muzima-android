package com.muzima.view.forms;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.RegistrationFormsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.collections.AvailableForms;

public class RegistrationFormsActivity extends SherlockActivity {
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
            startRegistrationView(availableForms.get(0));
        } else {
            prepareRegistrationAdapter(formController, availableForms);
        }
    }

    private void prepareRegistrationAdapter(FormController formController, AvailableForms availableForms) {
        registrationFormsAdapter = new RegistrationFormsAdapter(getApplicationContext(), R.layout.item_forms_list,
                formController, availableForms);
        list = (ListView) findViewById(R.id.list);
        list.setOnItemClickListener(startRegistrationOnClick());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
                startRegistrationView(form);
            }
        };
    }

    private void startRegistrationView(AvailableForm form) {
        startActivity(new FormViewIntent(this, form, new Patient()));
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
