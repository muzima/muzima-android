package com.muzima.view.cohort;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.actionbarsherlock.view.Menu;
import com.muzima.R;
import com.muzima.view.preferences.CohortPrefActivity;


public class CohortPrefixWizardActivity extends CohortPrefActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button nextButton = (Button) findViewById(R.id.next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToNextActivity();
            }
        });
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_cohort_prefix_wizard;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        disableSettingsMenu(menu);
        return true;
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(getApplicationContext(), CohortWizardActivity.class);
        startActivity(intent);
        finish();
    }
}