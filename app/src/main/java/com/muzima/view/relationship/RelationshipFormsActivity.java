/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.view.relationship;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.relationships.RelationshipFormsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.model.AvailableForm;
import com.muzima.model.collections.AvailableForms;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.forms.FormViewIntent;
import com.muzima.view.patients.PatientSummaryActivity;

import java.util.UUID;

import static com.muzima.view.relationship.RelationshipsListActivity.INDEX_PATIENT;

public class RelationshipFormsActivity extends AppCompatActivity {
    private RelationshipFormsAdapter relationshipFormsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_relationship_form_list);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = (int) (displayMetrics.heightPixels*0.9);
        int width = (int) (displayMetrics.widthPixels*0.9);
        getWindow().setLayout(width, height);

        FormController formController = ((MuzimaApplication) getApplicationContext()).getFormController();
        ObservationController observationController = ((MuzimaApplication) getApplicationContext()).getObservationController();
        AvailableForms availableForms = getRelationshipForms(formController);
        if (isOnlyOneRelationshipFormAvailable(availableForms)) {
            startWebViewActivity(availableForms.get(0));
        } else {
            prepareRelationshipAdapter(formController, availableForms, observationController);
        }
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
    }

    private void prepareRelationshipAdapter(FormController formController, AvailableForms availableForms, ObservationController observationController) {
        relationshipFormsAdapter = new RelationshipFormsAdapter(this, R.layout.item_forms_list,
                formController, availableForms, observationController);
        ListView list = findViewById(R.id.list);
        list.setOnItemClickListener(startRelationshipOnClick());
        list.setAdapter(relationshipFormsAdapter);
        relationshipFormsAdapter.reloadData();
        if(availableForms.size() == 0){
            TextView noFormsMessage = findViewById(R.id.no_forms_msg);
            noFormsMessage.setText(R.string.info_forms_unavailable);
            noFormsMessage.setVisibility(View.VISIBLE);
        }
    }

    private boolean isOnlyOneRelationshipFormAvailable(AvailableForms availableForms) {
        return availableForms.size() == 1;
    }

    private AdapterView.OnItemClickListener startRelationshipOnClick() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AvailableForm form = relationshipFormsAdapter.getItem(position);
                startWebViewActivity(form);
            }
        };
    }

    private void startWebViewActivity(AvailableForm form) {
        Patient patient = new Patient();
        String uuid = String.valueOf(UUID.randomUUID());
        patient.setUuid(uuid);

        Patient indexPatient = (Patient) getIntent().getSerializableExtra(INDEX_PATIENT);

        Intent intent = new FormViewIntent(this, form, patient , true);
        intent.putExtra(INDEX_PATIENT, indexPatient);
        startActivity(intent);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(PatientSummaryActivity.PATIENT, patient);
        setResult(0, resultIntent);
        finish();
    }

    private AvailableForms getRelationshipForms(FormController formController) {
        AvailableForms availableForms = null;
        try {
            availableForms = formController.getDownloadedRelationshipForms();
        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(), "Error while retrieving relationship forms from Lucene");
        }
        return availableForms;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return false;
        }
    }
}

