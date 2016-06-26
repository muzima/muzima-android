/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.Toast;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;
import com.muzima.adapters.cohort.CohortPrefixPrefAdapter;
import com.muzima.adapters.concept.AutoCompleteCohortPrefixAdapter;
import com.muzima.api.model.Cohort;
import com.muzima.service.CohortPrefixPreferenceService;
import com.muzima.view.BaseFragmentActivity;
import com.muzima.view.HelpActivity;

public class CohortPreferenceActivity extends BaseFragmentActivity {
    protected CohortPrefixPrefAdapter prefAdapter;
    private AutoCompleteCohortPrefixAdapter autoCompleteCohortPrefixAdapter;
    private AutoCompleteTextView cohortPrefix;
    private CohortPrefixPreferenceService preferenceService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());

        preferenceService = new CohortPrefixPreferenceService(this);

        ListView cohortPrefList = (ListView) findViewById(R.id.cohort_pref_list);
        prefAdapter = new CohortPrefixPrefAdapter(this, R.layout.item_preference, this);
        cohortPrefList.setEmptyView(findViewById(R.id.no_data_msg));
        cohortPrefList.setAdapter(prefAdapter);


        cohortPrefix = (AutoCompleteTextView) findViewById(R.id.prefix_add_prefix);
        autoCompleteCohortPrefixAdapter = new AutoCompleteCohortPrefixAdapter(getApplicationContext(), R.layout.item_option_autocomplete, cohortPrefix);
        cohortPrefix.setAdapter(autoCompleteCohortPrefixAdapter);

        cohortPrefix.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                Cohort selectedCohort = (Cohort) parent.getItemAtPosition(position);
                cohortPrefix.setText(selectedCohort.getName());
            }
        });
    }

    protected int getContentView() {
        return R.layout.activity_cohort_pref;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                Intent intent = new Intent(this, HelpActivity.class);
                intent.putExtra(HelpActivity.HELP_TYPE, HelpActivity.COHORT_PREFIX_HELP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefAdapter.reloadData();
    }

    public void addPrefix(View view) {
        String newPrefix = cohortPrefix.getText().toString();
        if (newPrefix.length() == 0) {
            Toast.makeText(this, "You can't add an empty prefix", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!preferenceService.addCohortPrefix(newPrefix)) {
            Toast.makeText(this, "Prefix already exists", Toast.LENGTH_SHORT).show();
        }
        prefAdapter.reloadData();
        cohortPrefix.clearListSelection();
    }


    public void onDeletePreferenceClick(String pref) {
        preferenceService.deleteCohortPrefix(pref);
        prefAdapter.reloadData();
    }
}
