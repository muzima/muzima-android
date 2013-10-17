package com.muzima.view.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.concept.AutoCompleteConceptAdapter;
import com.muzima.adapters.concept.SelectedConceptAdapter;
import com.muzima.api.model.Concept;
import com.muzima.search.api.util.StringUtil;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.HelpActivity;

import static com.muzima.utils.Constants.DataSyncServiceConstants.*;
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
        selectedConceptAdapter = new SelectedConceptAdapter(getApplicationContext(), R.layout.item_concept_list,
                ((MuzimaApplication)getApplicationContext()).getConceptController());
        selectedConceptListView.setAdapter(selectedConceptAdapter);

        autoCompleteConceptTextView = (AutoCompleteTextView) findViewById(R.id.concept_add_concept);
        autoCompleteConceptAdapter = new AutoCompleteConceptAdapter(getApplicationContext(), R.layout.item_concept_autocomplete);
        autoCompleteConceptTextView.setAdapter(autoCompleteConceptAdapter);
        autoCompleteConceptTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                Concept selectedConcept = (Concept) parent.getItemAtPosition(position);
                selectedConceptAdapter.add(selectedConcept);
                selectedConceptAdapter.notifyDataSetChanged();
                autoCompleteConceptTextView.setText(StringUtil.EMPTY);
            }
        });
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
