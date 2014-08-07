/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view;

import android.os.Bundle;
import android.widget.TextView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;

public class HelpActivity extends BaseActivity {

    public static final String HELP_TYPE = "HELP_TYPE";
    public static final int COHORT_WIZARD_HELP = 1;
    public static final int COHORT_PREFIX_HELP = 2;
    public static final int CUSTOM_CONCEPT_HELP = 3;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
        setHelpContent();
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.help, menu);
        super.onCreateOptionsMenu(menu);
        removeHelpMenu(menu);
        return true;
	}

    private void removeHelpMenu(Menu menu) {
        MenuItem menuSettings = menu.findItem(R.id.action_help);
        menuSettings.setVisible(false);
    }
}
