package com.muzima.view.forms;

import android.app.Activity;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import com.muzima.api.model.Patient;
import com.muzima.view.patients.PatientSummaryActivity;
import com.muzima.view.relationship.RelationshipFormsActivity;

public class RelationshipComponent {

    private static final int CREATE_RELATIONSHIP_PERSON_REQUEST_CODE = 0x0000202;
    private final Activity activity;

    private String sectionName;
    private String personUuidField;

    public RelationshipComponent(Activity activity){
        this.activity = activity;
    }

    @JavascriptInterface
    public void createRelationshipPerson(String sectionName, String personUuidField){
        this.sectionName = sectionName;
        this.personUuidField = personUuidField;

        Intent intent = new Intent(activity, RelationshipFormsActivity.class);
        activity.startActivityForResult(intent, CREATE_RELATIONSHIP_PERSON_REQUEST_CODE);
    }

    public static CreateRelationshipPersonResult parseActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == CREATE_RELATIONSHIP_PERSON_REQUEST_CODE) {
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
