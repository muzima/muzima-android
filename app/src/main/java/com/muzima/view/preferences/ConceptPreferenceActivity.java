/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.Toast;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.concept.AutoCompleteConceptAdapter;
import com.muzima.adapters.concept.SelectedConceptAdapter;
import com.muzima.api.model.Concept;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.ActionBar;

import com.muzima.utils.Constants;

public class ConceptPreferenceActivity extends BroadcastListenerActivity {
    private SelectedConceptAdapter selectedConceptAdapter;
    private ListView selectedConceptListView;
    private AutoCompleteTextView autoCompleteConceptTextView;
    private boolean actionModeActive = false;
    private ActionMode actionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        super.onCreate(savedInstanceState);
        setContentView(getContentView());

        LayoutInflater inflator = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.concept_autocomplete_textview_action, null);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(v);

        selectedConceptListView = findViewById(R.id.concept_preference_list);
        final MuzimaApplication applicationContext = (MuzimaApplication) getApplicationContext();
        selectedConceptAdapter = new SelectedConceptAdapter(this, R.layout.item_concept_list,
                (applicationContext).getConceptController());
        selectedConceptListView.setAdapter(selectedConceptAdapter);
        selectedConceptListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        selectedConceptListView.setSelected(true);
        selectedConceptListView.setClickable(true);
        selectedConceptListView.setEmptyView(findViewById(R.id.no_concept_added));
        selectedConceptListView.setOnItemClickListener(selectedConceptOnClickListener());
        autoCompleteConceptTextView = v.findViewById(R.id.concept_add_concept);
        AutoCompleteConceptAdapter autoCompleteConceptAdapter = new AutoCompleteConceptAdapter(this, R.layout.item_option_autocomplete, autoCompleteConceptTextView);
        autoCompleteConceptTextView.setAdapter(autoCompleteConceptAdapter);
        autoCompleteConceptTextView.setOnItemClickListener(autoCompleteOnClickListener());


        // this can happen on orientation change
        if (actionModeActive) {
            actionMode = startActionMode(new DeleteConceptsActionModeCallback());
            actionMode.setTitle(String.valueOf(getSelectedConcepts().size()));
        }
        logEvent("VIEW_CONCEPT_PREFERENCE");
    }

    private AdapterView.OnItemClickListener selectedConceptOnClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!actionModeActive) {
                    actionMode = startActionMode(new DeleteConceptsActionModeCallback());
                    actionModeActive = true;
                }
                int selectedConceptsCount = getSelectedConcepts().size();
                if (selectedConceptsCount == 0 && actionModeActive) {
                    actionMode.finish();
                }
                actionMode.setTitle(String.valueOf(selectedConceptsCount));
            }
        };
    }

    private AdapterView.OnItemClickListener autoCompleteOnClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                Concept selectedConcept = (Concept) parent.getItemAtPosition(position);
                if (selectedConceptAdapter.doesConceptAlreadyExist(selectedConcept)) {
                    Log.e(getClass().getSimpleName(), "Concept Already exists");
                    Toast.makeText(ConceptPreferenceActivity.this, "Concept " + selectedConcept.getName() + " already exists", Toast.LENGTH_SHORT).show();
                } else {
                    selectedConceptAdapter.addConcept(selectedConcept);
                    selectedConceptAdapter.notifyDataSetChanged();
                }
                autoCompleteConceptTextView.setText(StringUtils.EMPTY);
            }
        };
    }

    protected int getContentView() {
        return R.layout.activity_concept_preference;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionmode_menu_close, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_close:
                autoCompleteConceptTextView.setText(StringUtils.EMPTY);
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        selectedConceptAdapter.reloadData();
    }

    @Override
    protected void onReceive(Context context, Intent intent){
           super.onReceive(context, intent);

        int syncStatus = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);

        if (syncType == Constants.DataSyncServiceConstants.SYNC_TEMPLATES) {
            if (syncStatus == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                selectedConceptAdapter.reloadData();
            }
        }
    }

    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.layout_list, container, false);
    }

    final class DeleteConceptsActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            getMenuInflater().inflate(R.menu.actionmode_menu_delete, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_delete:
                    List<Concept> selectedConcepts = getSelectedConcepts();
                    selectedConceptAdapter.removeAll(selectedConcepts);
                    onCompleteOfConceptDelete(selectedConcepts.size());
            }
            return false;
        }

        private void onCompleteOfConceptDelete(int numberOfDeletedConcepts) {
            endActionMode();
            selectedConceptListView.clearChoices();
            selectedConceptAdapter.reloadData();
            Toast.makeText(getApplicationContext(), getString(R.string.info_concept_delete_success, numberOfDeletedConcepts), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            selectedConceptAdapter.clearSelectedForms();
        }
    }

    private void endActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private List<Concept> getSelectedConcepts() {
        List<Concept> concepts = new ArrayList<>();
        SparseBooleanArray checkedItemPositions = selectedConceptListView.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.valueAt(i)) {
                concepts.add(((Concept) selectedConceptListView.getItemAtPosition(checkedItemPositions.keyAt(i))));
            }
        }
        return concepts;
    }
}
