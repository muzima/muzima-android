package com.muzima.view.forms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;
import com.muzima.adapters.forms.AllAvailableFormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.search.api.util.StringUtil;
import com.muzima.service.DataSyncService;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.NetworkUtils;

import java.util.Date;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.CREDENTIALS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.FROM_IDS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TEMPLATES;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;

public class AllAvailableFormsListFragment extends FormsListFragment {
    private static final String TAG = "AllAvailableFormsListFragment";

    public static final String FORMS_METADATA_LAST_SYNCED_TIME = "formsMetadataSyncedTime";
    public static final long NOT_SYNCED_TIME = -1;

    private ActionMode actionMode;
    private boolean actionModeActive = false;
    private OnTemplateDownloadComplete templateDownloadCompleteListener;
    private TextView syncText;
    private boolean newFormsSyncInProgress;

    public static AllAvailableFormsListFragment newInstance(FormController formController) {
        AllAvailableFormsListFragment f = new AllAvailableFormsListFragment();
        f.formController = formController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (listAdapter == null) {
            listAdapter = new AllAvailableFormsAdapter(getActivity(), R.layout.item_forms_list, formController);
        }
        noDataMsg = getActivity().getResources().getString(R.string.no_new_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_new_form_tip);

        // this can happen on orientation change
        if (actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new NewFormsActionModeCallback());
            actionMode.setTitle(String.valueOf(((AllAvailableFormsAdapter) listAdapter).getSelectedForms().size()));
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.layout_synced_list, container, false);
        syncText = (TextView) view.findViewById(R.id.sync_text);
        updateSyncTime();
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (!actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new NewFormsActionModeCallback());
            actionModeActive = true;
        }
        ((AllAvailableFormsAdapter) listAdapter).onListItemClick(position);
        int numOfSelectedForms = ((AllAvailableFormsAdapter) listAdapter).getSelectedForms().size();
        if (numOfSelectedForms == 0 && actionModeActive) {
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numOfSelectedForms));
    }

    public void onFormTemplateDownloadFinish(){
        if (templateDownloadCompleteListener != null) {
            templateDownloadCompleteListener.onTemplateDownloadComplete();
        }
        listAdapter.reloadData();
    }

    public void onFormMetaDataDownloadFinish() {
        newFormsSyncInProgress = false;
        listAdapter.reloadData();
        updateSyncTime();
    }

    public void onFormMetaDataDownloadStart() {
        newFormsSyncInProgress = true;

    }

    @Override
    public void synchronizationStarted() {
    }

    public final class NewFormsActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            getSherlockActivity().getSupportMenuInflater().inflate(R.menu.actionmode_menu_download, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_download:
                    if (newFormsSyncInProgress) {
                        Toast.makeText(getActivity(), "Action not allowed while sync is in progress", Toast.LENGTH_SHORT).show();
                        if (AllAvailableFormsListFragment.this.actionMode != null) {
                            AllAvailableFormsListFragment.this.actionMode.finish();
                        }
                        break;
                    }

                    if (!NetworkUtils.isConnectedToNetwork(getActivity())) {
                        Toast.makeText(getActivity(), "No connection found, please connect your device and try again", Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    syncAllFormTemplatesInBackgroundService();

                    if (AllAvailableFormsListFragment.this.actionMode != null) {
                        AllAvailableFormsListFragment.this.actionMode.finish();
                    }
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            ((AllAvailableFormsAdapter) listAdapter).clearSelectedForms();
        }
    }

    private void syncAllFormTemplatesInBackgroundService() {
        Intent intent = new Intent(getActivity(), DataSyncService.class);
        intent.putExtra(SYNC_TYPE, SYNC_TEMPLATES);
        intent.putExtra(CREDENTIALS, getCredentials());
        intent.putExtra(FROM_IDS, getSelectedFormsArray());
        ((FormsActivity) getActivity()).showProgressBar();
        getActivity().startService(intent);
    }

    private String[] getCredentials() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String usernameKey = getResources().getString(R.string.preference_username);
        String passwordKey = getResources().getString(R.string.preference_password);
        String serverKey = getResources().getString(R.string.preference_server);
        String[] credentials = new String[]{settings.getString(usernameKey, StringUtil.EMPTY),
                settings.getString(passwordKey, StringUtil.EMPTY),
                settings.getString(serverKey, StringUtil.EMPTY)};
        return credentials;
    }

    public void setTemplateDownloadCompleteListener(OnTemplateDownloadComplete templateDownloadCompleteListener) {
        this.templateDownloadCompleteListener = templateDownloadCompleteListener;
    }

    public interface OnTemplateDownloadComplete {
        public void onTemplateDownloadComplete();
    }

    private String[] getSelectedFormsArray() {
        List<String> selectedForms = ((AllAvailableFormsAdapter) listAdapter).getSelectedForms();
        String[] selectedFormUuids = new String[selectedForms.size()];
        return selectedForms.toArray(selectedFormUuids);
    }

    private void updateSyncTime() {
        SharedPreferences pref = getActivity().getSharedPreferences(Constants.SYNC_PREF, Context.MODE_PRIVATE);
        long lastSyncedTime = pref.getLong(FORMS_METADATA_LAST_SYNCED_TIME, NOT_SYNCED_TIME);
        String lastSyncedMsg = "Not synced yet";
        if (lastSyncedTime != NOT_SYNCED_TIME) {
            lastSyncedMsg = "Last synced on: " + DateUtils.getFormattedDateTime(new Date(lastSyncedTime));
        }
        Log.e(TAG, lastSyncedMsg);
        syncText.setText(lastSyncedMsg);
    }
}
