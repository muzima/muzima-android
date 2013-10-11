package com.muzima.view;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import com.muzima.R;

public class HelpActivity extends Activity {

    public static final String HELP_TYPE = "HELP_TYPE";
    public static final String COHORT_WIZARD_HELP = "COHORT_WIZARD_HELP";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
        setHelpContent();
        // Show the Up button in the action bar.
		setupActionBar();
		
	}

    private void setHelpContent() {
        TextView helpContentView = (TextView)findViewById(R.id.helpContent);
        String helpType = getIntent().getStringExtra(HELP_TYPE);
        if(COHORT_WIZARD_HELP.equals(helpType)){
            helpContentView.setText(getResources().getText(R.string.cohort_wizard_help));
            setTitle(R.string.cohort_wizard_help_title);
        }
    }

    /**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.help, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
            finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
