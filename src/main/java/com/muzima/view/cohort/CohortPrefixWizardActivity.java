package com.muzima.view.cohort;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.muzima.R;
import com.muzima.view.BroadcastListenerActivity;


public class CohortPrefixWizardActivity extends BroadcastListenerActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cohort_prefix_wizard);

        Button nextButton = (Button) findViewById(R.id.next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CohortWizardActivity.class);
                startActivity(intent);
            }
        });
    }
}