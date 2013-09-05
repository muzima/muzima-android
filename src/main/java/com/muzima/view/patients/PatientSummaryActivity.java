package com.muzima.view.patients;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.muzima.R;
import com.muzima.view.forms.PatientFormsActivity;

public class PatientSummaryActivity extends Activity {
    public static final String PATIENT_ID = "patientId";

    private String patientId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client_summary);

        Bundle intentExtras = getIntent().getExtras();
        if(intentExtras != null){
            patientId = intentExtras.getString(PATIENT_ID);
        }

        View observations = findViewById(R.id.observations);
        observations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PatientSummaryActivity.this, PatientObservationsActivity.class);
                intent.putExtra(PATIENT_ID, patientId);
                startActivity(intent);
                overridePendingTransition(R.anim.push_in_from_right, R.anim.push_out_to_left);
            }
        });
        // Show the Up button in the action bar.
		setupActionBar();
    }

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle("♀ Client Name, ID#");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.client_summary, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
            overridePendingTransition(R.anim.push_in_from_left, R.anim.push_out_to_right);
            return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void showForms(View v) {
        Intent intent = new Intent(this, PatientFormsActivity.class);
        intent.putExtra(PatientSummaryActivity.PATIENT_ID, patientId);
        startActivity(intent);
        overridePendingTransition(R.anim.push_in_from_right, R.anim.push_out_to_left);
	}
}
