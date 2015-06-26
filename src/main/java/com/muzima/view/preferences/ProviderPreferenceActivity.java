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
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.concept.AutoCompleteProviderAdapter;
import com.muzima.adapters.concept.SelectedProviderAdapter;
import com.muzima.api.model.Provider;
import com.muzima.search.api.util.StringUtil;
import com.muzima.utils.Constants;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.HelpActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vikas on 16/03/15.
 */
public class ProviderPreferenceActivity extends BroadcastListenerActivity {
    private static final String TAG = ProviderPreferenceActivity.class.getSimpleName();
    private SelectedProviderAdapter selectedProviderAdapter;
    private ListView selectedProviderListView;
    private AutoCompleteTextView autoCompleteProvidersTextView;
    private AutoCompleteProviderAdapter autoCompleteProviderAdapter;
    private boolean actionModeActive = false;
    private ActionMode actionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());

        selectedProviderListView = (ListView) findViewById(R.id.provider_preference_list);
        final MuzimaApplication applicationContext = (MuzimaApplication) getApplicationContext();
        selectedProviderAdapter = new SelectedProviderAdapter(this, R.layout.item_provider_list,
                (applicationContext).getProviderController());
        selectedProviderListView.setAdapter(selectedProviderAdapter);
        selectedProviderListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        selectedProviderListView.setSelected(true);
        selectedProviderListView.setClickable(true);
        selectedProviderListView.setEmptyView(findViewById(R.id.no_provider_added));
        selectedProviderListView.setOnItemClickListener(selectedProviderOnClickListener());
        autoCompleteProvidersTextView = (AutoCompleteTextView) findViewById(R.id.add_provider);
        autoCompleteProviderAdapter = new AutoCompleteProviderAdapter(applicationContext, R.layout.item_option_autocomplete, autoCompleteProvidersTextView);
        autoCompleteProvidersTextView.setAdapter(autoCompleteProviderAdapter);
        autoCompleteProvidersTextView.setOnItemClickListener(autoCompleteOnClickListener());

        // this can happen on orientation change
        if (actionModeActive) {
            actionMode = getSherlock().startActionMode(new DeleteProvidersActionModeCallback());
            actionMode.setTitle(String.valueOf(getSelectedProviders().size()));
        }
    }

    private AdapterView.OnItemClickListener selectedProviderOnClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!actionModeActive) {
                    actionMode = getSherlock().startActionMode(new DeleteProvidersActionModeCallback());
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
                    Log.e(TAG, "Providers Already exists");
                    Toast.makeText(ProviderPreferenceActivity.this, "Provider " + selectedProvider.getName() + " already exists", Toast.LENGTH_SHORT).show();
                } else {
                    selectedProviderAdapter.addProvider(selectedProvider);
                    selectedProviderAdapter.notifyDataSetChanged();
                }
                autoCompleteProvidersTextView.setText(StringUtil.EMPTY);
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

    public final class DeleteProvidersActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            getSherlock().getMenuInflater().inflate(R.menu.actionmode_menu_delete, menu);
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
            Toast.makeText(getApplicationContext(), numberOfDeletedProviders +" Providers deleted successfully!!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            selectedProviderAdapter.clearSelectedProviders();
        }
    }

    public void endActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private List<Provider> getSelectedProviders() {
        List<Provider> providers = new ArrayList<Provider>();
        SparseBooleanArray checkedItemPositions = selectedProviderListView.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.valueAt(i)) {
                providers.add(((Provider) selectedProviderListView.getItemAtPosition(checkedItemPositions.keyAt(i))));
            }
        }
        return providers;
    }
}
