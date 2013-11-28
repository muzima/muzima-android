package com.muzima.view.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.Toast;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.concept.AutoCompleteConceptAdapter;
import com.muzima.adapters.concept.SelectedConceptAdapter;
import com.muzima.api.model.Concept;
import com.muzima.search.api.util.StringUtil;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.HelpActivity;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_STATUS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TEMPLATES;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR;

public class ConceptPreferenceActivity extends BroadcastListenerActivity {
    private static final String TAG = ConceptPreferenceActivity.class.getSimpleName();
    private SelectedConceptAdapter selectedConceptAdapter;
    private ListView selectedConceptListView;
    private AutoCompleteTextView autoCompleteConceptTextView;
    private AutoCompleteConceptAdapter autoCompleteConceptAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());

        selectedConceptListView = (ListView) findViewById(R.id.concept_preference_list);
        MuzimaApplication applicationContext = (MuzimaApplication) getApplicationContext();
        selectedConceptAdapter = new SelectedConceptAdapter(applicationContext, R.layout.item_concept_list,
                (applicationContext).getConceptController());
        selectedConceptListView.setAdapter(selectedConceptAdapter);
        selectedConceptListView.setEmptyView(findViewById(R.id.no_concept_added));
        autoCompleteConceptTextView = (AutoCompleteTextView) findViewById(R.id.concept_add_concept);
        autoCompleteConceptAdapter = new AutoCompleteConceptAdapter(applicationContext, R.layout.item_option_autocomplete, autoCompleteConceptTextView);
        autoCompleteConceptTextView.setAdapter(autoCompleteConceptAdapter);
        autoCompleteConceptTextView.setOnItemClickListener(autoCompleteOnClickListener());
    }

    private AdapterView.OnItemClickListener autoCompleteOnClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                Concept selectedConcept = (Concept) parent.getItemAtPosition(position);
                if (selectedConceptAdapter.doesConceptAlreadyExist(selectedConcept)) {
                    Log.e(TAG, "Concept Already exists");
                    Toast.makeText(ConceptPreferenceActivity.this, "Concept " + selectedConcept.getName() + " already exists", Toast.LENGTH_SHORT).show();
                } else {
                    selectedConceptAdapter.addConcept(selectedConcept);
                    selectedConceptAdapter.notifyDataSetChanged();
                }
                autoCompleteConceptTextView.setText(StringUtil.EMPTY);
            }
        };
    }

    protected int getContentView() {
        return R.layout.activity_concept_preference;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                Intent intent = new Intent(this, HelpActivity.class);
                intent.putExtra(HelpActivity.HELP_TYPE, HelpActivity.CUSTOM_CONCEPT_HELP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        selectedConceptAdapter.reloadData();
    }

    @Override
    protected void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        int syncStatus = intent.getIntExtra(SYNC_STATUS, UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(SYNC_TYPE, -1);

        if(syncType == SYNC_TEMPLATES){
            if(syncStatus == SUCCESS){
                selectedConceptAdapter.reloadData();
            }
        }
    }
}
