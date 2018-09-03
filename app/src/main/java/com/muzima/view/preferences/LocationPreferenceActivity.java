/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
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
import com.muzima.adapters.concept.AutoCompleteLocationAdapter;
import com.muzima.adapters.concept.SelectedLocationAdapter;
import com.muzima.api.model.Location;
import com.muzima.utils.Constants;
import com.muzima.utils.StringUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.HelpActivity;

import java.util.ArrayList;
import java.util.List;

public class LocationPreferenceActivity extends BroadcastListenerActivity {
    private SelectedLocationAdapter selectedLocationAdapter;
    private ListView selectedLocationListView;
    private AutoCompleteTextView autoCompleteLocationsTextView;
    private boolean actionModeActive = false;
    private ActionMode actionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());

        selectedLocationListView = findViewById(R.id.location_preference_list);
        final MuzimaApplication applicationContext = (MuzimaApplication) getApplicationContext();
        selectedLocationAdapter = new SelectedLocationAdapter(this, R.layout.item_location_list,
                (applicationContext).getLocationController());
        selectedLocationListView.setAdapter(selectedLocationAdapter);
        selectedLocationListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        selectedLocationListView.setSelected(true);
        selectedLocationListView.setClickable(true);
        selectedLocationListView.setEmptyView(findViewById(R.id.no_location_added));
        selectedLocationListView.setOnItemClickListener(selectedLocationOnClickListener());
        autoCompleteLocationsTextView = findViewById(R.id.add_location);
        AutoCompleteLocationAdapter autoCompleteLocationAdapter = new AutoCompleteLocationAdapter(applicationContext, R.layout.item_option_autocomplete, autoCompleteLocationsTextView);
        autoCompleteLocationsTextView.setAdapter(autoCompleteLocationAdapter);
        autoCompleteLocationsTextView.setOnItemClickListener(autoCompleteOnClickListener());

        // this can happen on orientation change
        if (actionModeActive) {
            actionMode = startActionMode(new DeleteLocationsActionModeCallback());
            actionMode.setTitle(String.valueOf(getSelectedLocations().size()));
        }
    }

    private AdapterView.OnItemClickListener selectedLocationOnClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!actionModeActive) {
                    actionMode = startActionMode(new DeleteLocationsActionModeCallback());
                    actionModeActive = true;
                }
                int selectedLocationsCount = getSelectedLocations().size();
                if (selectedLocationsCount == 0 && actionModeActive) {
                    actionMode.finish();
                }
                actionMode.setTitle(String.valueOf(selectedLocationsCount));
            }
        };
    }

    private AdapterView.OnItemClickListener autoCompleteOnClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                Location selectedLocation = (Location) parent.getItemAtPosition(position);
                if (selectedLocationAdapter.doesLocationAlreadyExist(selectedLocation)) {
                    Log.e(getClass().getSimpleName(), "Locations Already exists");
                    Toast.makeText(LocationPreferenceActivity.this, "Location " + selectedLocation.getName() + " already exists", Toast.LENGTH_SHORT).show();
                } else {
                    selectedLocationAdapter.addLocation(selectedLocation);
                    selectedLocationAdapter.notifyDataSetChanged();
                }
                autoCompleteLocationsTextView.setText(StringUtils.EMPTY);
            }
        };
    }

    protected int getContentView() {
        return R.layout.activity_location_preference;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                Intent intent = new Intent(this, HelpActivity.class);
                intent.putExtra(HelpActivity.HELP_TYPE, HelpActivity.CUSTOM_LOCATION_HELP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        selectedLocationAdapter.reloadData();
    }

    @Override
    protected void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        int syncStatus = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);

        if (syncType == Constants.DataSyncServiceConstants.SYNC_TEMPLATES) {
            if (syncStatus == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                selectedLocationAdapter.reloadData();
            }
        }
    }

    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.layout_list, container, false);
    }

    final class DeleteLocationsActionModeCallback implements ActionMode.Callback {

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
                    List<Location> selectedLocations = getSelectedLocations();
                    selectedLocationAdapter.removeAll(selectedLocations);
                    onCompleteOfLocationDelete(selectedLocations.size());
            }
            return false;
        }

        private void onCompleteOfLocationDelete(int numberOfDeletedLocations) {
            endActionMode();
            selectedLocationListView.clearChoices();
            selectedLocationAdapter.reloadData();
            Toast.makeText(getApplicationContext(), getString(R.string.info_location_delete_success, numberOfDeletedLocations), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            selectedLocationAdapter.clearSelectedLocations();
        }
    }

    private void endActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private List<Location> getSelectedLocations() {
        List<Location> locations = new ArrayList<>();
        SparseBooleanArray checkedItemPositions = selectedLocationListView.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.valueAt(i)) {
                locations.add(((Location) selectedLocationListView.getItemAtPosition(checkedItemPositions.keyAt(i))));
            }
        }
        return locations;
    }
}

