/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.app.Activity;
import android.content.Intent;
import android.webkit.JavascriptInterface;

public class EncounterMiniFormCreatorComponent {
    private final Activity activity;

    public EncounterMiniFormCreatorComponent(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void launchEncounterMiniForm(String patientUuid, String formUuid){
        Intent formIntent = new Intent(activity.getApplicationContext(), EncounterMiniFormActivity.class);
        formIntent.putExtra(EncounterMiniFormActivity.FORM_UUID, formUuid);
        formIntent.putExtra(EncounterMiniFormActivity.PATIENT_UUID, patientUuid);
        activity.startActivity(formIntent);
    }
}
