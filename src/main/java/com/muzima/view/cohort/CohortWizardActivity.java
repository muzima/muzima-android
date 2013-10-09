package com.muzima.view.cohort;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.cohort.AllCohortsAdapter;
import com.muzima.view.BroadcastListenerActivity;


public class CohortWizardActivity extends BroadcastListenerActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cohort_wizard);
        ListView listView = getListView();
        final AllCohortsAdapter cohortsAdapter = createAllCohortsAdapter();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                cohortsAdapter.onListItemClick(position);
            }
        });
        cohortsAdapter.reloadData();
        listView.setAdapter(cohortsAdapter);
    }

    private AllCohortsAdapter createAllCohortsAdapter() {
        return new AllCohortsAdapter(getApplicationContext(), R.layout.item_cohorts_list, ((MuzimaApplication) getApplicationContext()).getCohortController());
    }

    private ListView getListView() {
        return (ListView) findViewById(R.id.cohort_wizard_list);
    }
}
