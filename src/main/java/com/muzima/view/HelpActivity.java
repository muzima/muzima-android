package com.muzima.view;

import android.os.Bundle;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;

public class HelpActivity extends SherlockActivity {

    public static final String HELP_TYPE = "HELP_TYPE";
    public static final int COHORT_WIZARD_HELP = 1;
    public static final int COHORT_PREFIX_HELP = 2;
    public static final int CUSTOM_CONCEPT_HELP = 3;

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
        int helpType = getIntent().getIntExtra(HELP_TYPE, 0);
        switch (helpType){
            case COHORT_WIZARD_HELP:
                helpContentView.setText(getResources().getText(R.string.cohort_wizard_help));
                setTitle(R.string.cohort_wizard_help_title);
                break;
            case COHORT_PREFIX_HELP:
                helpContentView.setText(getResources().getText(R.string.cohort_prefix_help));
                setTitle(R.string.cohort_prefix_help_title);
                break;
            case CUSTOM_CONCEPT_HELP:
                helpContentView.setText(getResources().getText(R.string.custom_concept_help));
                setTitle(R.string.custom_concept_help_title);
                break;
        }
    }

    /**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.help, menu);
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
