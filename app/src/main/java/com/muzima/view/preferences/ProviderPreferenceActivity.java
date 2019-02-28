/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
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
import com.muzima.utils.StringUtils;
import android.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.concept.AutoCompleteProviderAdapter;
import com.muzima.adapters.concept.SelectedProviderAdapter;
import com.muzima.api.model.Provider;
import com.muzima.utils.Constants;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.HelpActivity;

import java.util.ArrayList;
import java.util.List;

public class ProviderPreferenceActivity extends BroadcastListenerActivity {
    private SelectedProviderAdapter selectedProviderAdapter;
    private ListView selectedProviderListView;
    private AutoCompleteTextView autoCompleteProvidersTextView;
    private boolean actionModeActive = false;
    private ActionMode actionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());

        selectedProviderListView = findViewById(R.id.provider_preference_list);
        final MuzimaApplication applicationContext = (MuzimaApplication) getApplicationContext();
        selectedProviderAdapter = new SelectedProviderAdapter(this, R.layout.item_provider_list,
                (applicationContext).getProviderController());
        selectedProviderListView.setAdapter(selectedProviderAdapter);
        selectedProviderListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        selectedProviderListView.setSelected(true);
        selectedProviderListView.setClickable(true);
        selectedProviderListView.setEmptyView(findViewById(R.id.no_provider_added));
        selectedProviderListView.setOnItemClickListener(selectedProviderOnClickListener());
        autoCompleteProvidersTextView = findViewById(R.id.add_provider);
        AutoCompleteProviderAdapter autoCompleteProviderAdapter = new AutoCompleteProviderAdapter(applicationContext, R.layout.item_option_autocomplete, autoCompleteProvidersTextView);
        autoCompleteProvidersTextView.setAdapter(autoCompleteProviderAdapter);
        autoCompleteProvidersTextView.setOnItemClickListener(autoCompleteOnClickListener());

        // this can happen on orientation change
        if (actionModeActive) {
            actionMode = startActionMode(new DeleteProvidersActionModeCallback());
            actionMode.setTitle(String.valueOf(getSelectedProviders().size()));
        }
    }

    private AdapterView.OnItemClickListener selectedProviderOnClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!actionModeActive) {
                    actionMode = startActionMode(new DeleteProvidersActionModeCallback());
                    actionModeActive = true;
                }
                int selectedProvidersCount = getSelectedProviders().size();
                if (selectedProvidersCount == 0 && actionModeActive) {
                    actionMode.finish();
                }
                actionMode.setTitle(String.valueOf(selectedProvidersCount));
            }
        };
    }

    private AdapterView.OnItemClickListener autoCompleteOnClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                Provider selectedProvider = (Provider) parent.getItemAtPosition(position);
                if (selectedProviderAdapter.doesProviderAlreadyExist(selectedProvider)) {
                    Log.e(getClass().getSimpleName(), "Providers Already exists");
                    Toast.makeText(ProviderPreferenceActivity.this, "Provider " + selectedProvider.getName() + " already exists", Toast.LENGTH_SHORT).show();
                } else {
                    selectedProviderAdapter.addProvider(selectedProvider);
                    selectedProviderAdapter.notifyDataSetChanged();
                }
                autoCompleteProvidersTextView.setText(StringUtils.EMPTY);
            }
        };
    }

    protected int getContentView() {
        return R.layout.activity_provider_preference;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                Intent intent = new Intent(this, HelpActivity.class);
                intent.putExtra(HelpActivity.HELP_TYPE, HelpActivity.CUSTOM_PROVIDER_HELP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        selectedProviderAdapter.reloadData();
    }

    @Override
    protected void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        int syncStatus = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);

        if (syncType == Constants.DataSyncServiceConstants.SYNC_TEMPLATES) {
            if (syncStatus == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                selectedProviderAdapter.reloadData();
            }
        }
    }

    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.layout_list, container, false);
    }

    final class DeleteProvidersActionModeCallback implements ActionMode.Callback {

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
                    List<Provider> selectedProviders = getSelectedProviders();
                    selectedProviderAdapter.removeAll(selectedProviders);
                    onCompleteOfProviderDelete(selectedProviders.size());
            }
            return false;
        }

        private void onCompleteOfProviderDelete(int numberOfDeletedProviders) {
            endActionMode();
            selectedProviderListView.clearChoices();
            selectedProviderAdapter.reloadData();
            Toast.makeText(getApplicationContext(), getString(R.string.info_provider_delete_success,numberOfDeletedProviders), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            selectedProviderAdapter.clearSelectedProviders();
        }
    }

    private void endActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private List<Provider> getSelectedProviders() {
        List<Provider> providers = new ArrayList<>();
        SparseBooleanArray checkedItemPositions = selectedProviderListView.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.valueAt(i)) {
                providers.add(((Provider) selectedProviderListView.getItemAtPosition(checkedItemPositions.keyAt(i))));
            }
        }
        return providers;
    }
}
