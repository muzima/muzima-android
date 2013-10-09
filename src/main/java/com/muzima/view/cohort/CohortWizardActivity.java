package com.muzima.view.cohort;

import android.os.Bundle;
import android.widget.ListView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.cohort.AllCohortsAdapter;
import com.muzima.view.BroadcastListenerActivity;


public class CohortWizardActivity extends BroadcastListenerActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cohort_wizard);
        ListView listView = (ListView) findViewById(R.id.cohort_wizard_list);
        AllCohortsAdapter cohortsAdapter = new AllCohortsAdapter(getApplicationContext(), R.layout.item_cohorts_list, ((MuzimaApplication) getApplicationContext()).getCohortController());
        cohortsAdapter.reloadData();
        listView.setAdapter(cohortsAdapter);
    }
}
