package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.muzima.R;

public class WizardFinishPreferenceService extends PreferenceService {

    private final SharedPreferences settings;

    public WizardFinishPreferenceService(Context context) {
        super(context);
        settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isWizardFinished(){
        String wizardFinishedKey = context.getResources().getString(R.string.preference_wizard_finished);

        return settings.getBoolean(wizardFinishedKey, false);
    }

    public void finishWizard(){
        setWizardFinished(true);
    }

    public void resetWizard(){
        setWizardFinished(false);
    }

    public void setWizardFinished(boolean wizardFinished) {
        String wizardFinishedKey = context.getResources().getString(R.string.preference_wizard_finished);
        settings.edit()
                .putBoolean(wizardFinishedKey, wizardFinished)
                .commit();
    }
}
