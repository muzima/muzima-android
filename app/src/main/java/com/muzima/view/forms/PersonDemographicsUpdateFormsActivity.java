package com.muzima.view.forms;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.relationships.RelationshipFormsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.collections.AvailableForms;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.patients.PatientSummaryActivity;

import static com.muzima.view.relationship.RelationshipsListActivity.INDEX_PATIENT;

public class PersonDemographicsUpdateFormsActivity extends AppCompatActivity {
    public static final String PERSON = "person";
    private RelationshipFormsAdapter relationshipFormsAdapter;
    private final ThemeUtils themeUtils = new ThemeUtils();
    private Patient person;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        super.onCreate(savedInstanceState);

        Person selectedRelatedPerson = (Person) getIntent().getSerializableExtra(PERSON);
        if(selectedRelatedPerson != null) {
            person = new Patient();
            person.setUuid(selectedRelatedPerson.getUuid());
            person.setBirthdate(selectedRelatedPerson.getBirthdate());
            person.setBirthdateEstimated(selectedRelatedPerson.getBirthdateEstimated());
            person.setGender(selectedRelatedPerson.getGender());
            person.setNames(selectedRelatedPerson.getNames());
        } else {
            person = (Patient) getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);
        }

        setContentView(R.layout.activity_relationship_form_list);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = (int) (displayMetrics.heightPixels*0.9);
        int width = (int) (displayMetrics.widthPixels*0.9);
        getWindow().setLayout(width, height);

        FormController formController = ((MuzimaApplication) getApplicationContext()).getFormController();
        AvailableForms availableForms = getPersonUpdateForms(formController);
        if (isOnlyOneRelationshipFormAvailable(availableForms)) {
            startWebViewActivity(availableForms.get(0));
        } else {
            prepareRelationshipAdapter(formController, availableForms);
        }
    }

    private void prepareRelationshipAdapter(FormController formController, AvailableForms availableForms) {
        relationshipFormsAdapter = new RelationshipFormsAdapter(this, R.layout.item_forms_list,
                formController, availableForms);
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

    private AvailableForms getPersonUpdateForms(FormController formController) {
        AvailableForms availableForms = null;
        try {
            availableForms = formController.getDownloadedPersonUpdateForms();
        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(), "Error while retrieving relationship forms from Lucene");
        }
        return availableForms;
    }

    private void startWebViewActivity(AvailableForm form) {
        Patient indexPatient = (Patient) getIntent().getSerializableExtra(INDEX_PATIENT);

        Intent intent = new FormViewIntent(this, form, person , true);
        intent.putExtra(INDEX_PATIENT, indexPatient);

        startActivity(intent);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(PatientSummaryActivity.PATIENT, person);
        setResult(0, resultIntent);
        finish();
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
}
