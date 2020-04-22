package com.muzima.view.forms;

import android.app.Activity;
import android.content.Intent;
import android.webkit.JavascriptInterface;

import static com.muzima.view.forms.EncounterMiniFormActivity.FORM_UUID;
import static com.muzima.view.forms.EncounterMiniFormActivity.PATIENT_UUID;

public class EncounterMiniFormCreatorComponent {
    private final Activity activity;

    public EncounterMiniFormCreatorComponent(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void launchEncounterMiniForm(String patientUuid, String formUuid){
        Intent formIntent = new Intent(activity.getApplicationContext(), EncounterMiniFormActivity.class);
        formIntent.putExtra(FORM_UUID, formUuid);
        formIntent.putExtra(PATIENT_UUID, patientUuid);
        activity.startActivity(formIntent);
    }
}
