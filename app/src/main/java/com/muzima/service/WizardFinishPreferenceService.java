/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.service;

import android.content.Context;
import android.util.Log;

import com.muzima.R;
import com.muzima.utils.MuzimaPreferences;

public class WizardFinishPreferenceService extends PreferenceService {

    public WizardFinishPreferenceService(Context context) {
        super(context);
    }

    public boolean isWizardFinished(){
        String wizardFinishedKey = context.getResources().getString(R.string.preference_wizard_finished);
        return MuzimaPreferences.getBooleanPreference(context, wizardFinishedKey, false);
    }

    public void finishWizard(){
        setWizardFinished(true);
    }

    public void resetWizard(){
        setWizardFinished(false);
    }

    private void setWizardFinished(boolean wizardFinished) {
        String wizardFinishedKey = context.getResources().getString(R.string.preference_wizard_finished);
        MuzimaPreferences.setBooleanPreference(context, wizardFinishedKey, wizardFinished);
    }
}
