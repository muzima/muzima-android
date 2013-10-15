package com.muzima.view.cohort;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import com.muzima.R;
import com.muzima.view.MainActivity;
import com.muzima.view.preferences.ConceptPreferenceActivity;


public class CustomConceptWizardActivity extends ConceptPreferenceActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Button nextButton = (Button) findViewById(R.id.next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markWizardHasEnded();
                navigateToNextActivity();
            }
        });

        Button previousButton = (Button) findViewById(R.id.previous);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToPreviousActivity();
            }
        });
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_custom_concept_wizard;
    }

    private void markWizardHasEnded() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String wizardFinishedKey = getResources().getString(R.string.preference_wizard_finished);
        settings.edit()
                .putBoolean(wizardFinishedKey, true)
                .commit();
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    private void navigateToPreviousActivity() {
        Intent intent = new Intent(getApplicationContext(), FormTemplateWizardActivity.class);
        startActivity(intent);
    }
}