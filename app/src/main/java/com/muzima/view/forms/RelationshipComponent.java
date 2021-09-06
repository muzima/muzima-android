package com.muzima.view.forms;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.webkit.JavascriptInterface;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.controller.PatientController;
import com.muzima.controller.PersonController;
import com.muzima.view.patients.PatientSummaryActivity;
import com.muzima.view.relationship.RelationshipFormsActivity;

import static com.muzima.view.relationship.RelationshipsListActivity.INDEX_PATIENT;

public class RelationshipComponent {

    private static final int CREATE_RELATIONSHIP_PERSON_REQUEST_CODE = 0x0000202;
    private static final int UPDATE_RELATIONSHIP_PERSON_REQUEST_CODE = 0x0000203;
    private final Activity activity;

    private String sectionName;
    private String personUuidField;
    private String personUuid;
    private Patient indexPatient;

    public RelationshipComponent(Activity activity, Patient indexPatient){
        this.activity = activity;
        this.indexPatient = indexPatient;
    }

    @JavascriptInterface
    public void createRelationshipPerson(String sectionName, String personUuidField){
        this.sectionName = sectionName;
        this.personUuidField = personUuidField;

        Intent intent = new Intent(activity, RelationshipFormsActivity.class);
        intent.putExtra(INDEX_PATIENT, indexPatient);
        activity.startActivityForResult(intent, CREATE_RELATIONSHIP_PERSON_REQUEST_CODE);
    }

    @JavascriptInterface
    public void updateRelationshipPerson(String sectionName, String personUuidField, String personUuid){
        this.sectionName = sectionName;
        this.personUuidField = personUuidField;
        this.personUuid = personUuid;

        try {
            Person person = ((MuzimaApplication) activity.getApplicationContext()).getPersonController().getPersonByUuid(personUuid);

            Intent intent = new Intent(activity, PersonDemographicsUpdateFormsActivity.class);
            intent.putExtra(INDEX_PATIENT, indexPatient);

            if(person != null) {
                intent.putExtra(PersonDemographicsUpdateFormsActivity.PERSON, person);
                activity.startActivityForResult(intent, UPDATE_RELATIONSHIP_PERSON_REQUEST_CODE);
            } else {
                Patient patient = ((MuzimaApplication) activity.getApplicationContext()).getPatientController().getPatientByUuid(personUuid);
                if(patient != null){
                    intent.putExtra(PatientSummaryActivity.PATIENT, patient);
                    activity.startActivityForResult(intent, UPDATE_RELATIONSHIP_PERSON_REQUEST_CODE);
                } else {
                    Log.e("RelationshipComponent","Could not load person");
                }
            }
        } catch (PersonController.PersonLoadException | PatientController.PatientLoadException e) {
            Log.e("RelationshipComponent","Could not load person",e);
        }
    }

    public static CreateRelationshipPersonResult parseActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == CREATE_RELATIONSHIP_PERSON_REQUEST_CODE || requestCode == UPDATE_RELATIONSHIP_PERSON_REQUEST_CODE) {
            if(intent.hasExtra(PatientSummaryActivity.PATIENT)) {
                Patient patient = (Patient) intent.getSerializableExtra(PatientSummaryActivity.PATIENT);
                return new CreateRelationshipPersonResult(patient.getUuid());
            }
        }
        return null;
    }

    public String getSectionName() {
        return sectionName;
    }

    public String getPersonUuidField() {
        return personUuidField;
    }
}
