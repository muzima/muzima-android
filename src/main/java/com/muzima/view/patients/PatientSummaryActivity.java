package com.muzima.view.patients;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.support.v4.app.NavUtils;
import com.muzima.R;
import com.muzima.api.model.Patient;
import com.muzima.view.ClientObservationsActivity;
import com.muzima.view.forms.PatientFormsActivity;

public class PatientSummaryActivity extends Activity {
    public static final String PATIENT_ID = "patientId";

    private String patientId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client_summary);
		// Show the Up button in the action bar.
		setupActionBar();

        Bundle intentExtras = getIntent().getExtras();
        if(intentExtras != null){
            patientId = intentExtras.getString(PATIENT_ID);
        }
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
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
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
	/** Called when the user clicks the Clients Encounters Button or Search Clients Observations Button */
	public void clientObservations(View view) {
		Intent intent = new Intent(this, ClientObservationsActivity.class);
		if (view.getId() == R.id.quickSearch) {
			intent.putExtra("quickSearch", "true");
		}
		startActivity(intent);
	}
}
