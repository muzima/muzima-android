package com.muzima.view.forms;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.NewFormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.listeners.DownloadListener;
import com.muzima.search.api.util.StringUtil;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.tasks.forms.DownloadFormTemplateTask;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.NetworkUtils;

import java.util.Date;
import java.util.List;

import static android.os.AsyncTask.Status.PENDING;
import static android.os.AsyncTask.Status.RUNNING;

public class NewFormsListFragment extends FormsListFragment implements DownloadListener<Integer[]> {
    private static final String TAG = "NewFormsListFragment";

    private static final String BUNDLE_SELECTED_FORMS = "selectedForms";
    public static final String FORMS_METADATA_LAST_SYNCED_TIME = "formsMetadataSyncedTime";
    public static final long NOT_SYNCED_TIME = -1;

    private ActionMode actionMode;
    private boolean actionModeActive = false;
    private DownloadFormTemplateTask formTemplateDownloadTask;
    private OnTemplateDownloadComplete templateDownloadCompleteListener;
    private TextView syncText;

    public static NewFormsListFragment newInstance(FormController formController) {
        NewFormsListFragment f = new NewFormsListFragment();
        f.formController = formController;
        f.setRetainInstance(true);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (listAdapter == null) {
            listAdapter = new NewFormsAdapter(getActivity(), R.layout.item_forms_list, formController);
        }
        noDataMsg = getActivity().getResources().getString(R.string.no_new_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_new_form_tip);

        // this can happen on orientation change
        if (actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new NewFormsActionModeCallback());
            actionMode.setTitle(String.valueOf(((NewFormsAdapter) listAdapter).getSelectedForms().size()));
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.layout_synced_list, container, false);
        syncText = (TextView) view.findViewById(R.id.sync_text);
        updateSyncText();
        return view;
    }

    @Override
    public void onDestroy() {
        if(formTemplateDownloadTask != null){
            formTemplateDownloadTask.cancel(false);
        }
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (!actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new NewFormsActionModeCallback());
            actionModeActive = true;
        }
        ((NewFormsAdapter) listAdapter).onListItemClick(position);
        int numOfSelectedForms = ((NewFormsAdapter) listAdapter).getSelectedForms().size();
        if (numOfSelectedForms == 0 && actionModeActive) {
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numOfSelectedForms));
    }

    @Override
    public void downloadTaskComplete(Integer[] result) {
        if (templateDownloadCompleteListener != null) {
            templateDownloadCompleteListener.onTemplateDownloadComplete(result);
        }
    }

    @Override
    public void formDownloadComplete(Integer[] status) {
        if(status[0] == DownloadMuzimaTask.SUCCESS){
            updateSyncText();
        }
        super.formDownloadComplete(status);
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
                    if(!NetworkUtils.isConnectedToNetwork(getActivity())){
                        Toast.makeText(getActivity(), "No connection found, please connect your device and try again", Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    if (formTemplateDownloadTask != null &&
                            (formTemplateDownloadTask.getStatus() == PENDING || formTemplateDownloadTask.getStatus() == RUNNING)) {
                        Toast.makeText(getActivity(), "Already fetching form templates, ignored the request", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
                    formTemplateDownloadTask = new DownloadFormTemplateTask((MuzimaApplication) getActivity().getApplication());
                    formTemplateDownloadTask.addDownloadListener(NewFormsListFragment.this);
                    String usernameKey = getResources().getString(R.string.preference_username);
                    String passwordKey = getResources().getString(R.string.preference_password);
                    String serverKey = getResources().getString(R.string.preference_server);
                    String[] credentials = new String[]{settings.getString(usernameKey, StringUtil.EMPTY),
                            settings.getString(passwordKey, StringUtil.EMPTY),
                            settings.getString(serverKey, StringUtil.EMPTY)};
                    formTemplateDownloadTask.execute(credentials, getSelectedFormsArray());
                    if (NewFormsListFragment.this.actionMode != null) {
                        NewFormsListFragment.this.actionMode.finish();
                    }
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            ((NewFormsAdapter) listAdapter).clearSelectedForms();
        }
    }

    public void setTemplateDownloadCompleteListener(OnTemplateDownloadComplete templateDownloadCompleteListener) {
        this.templateDownloadCompleteListener = templateDownloadCompleteListener;
    }

    public interface OnTemplateDownloadComplete {
        public void onTemplateDownloadComplete(Integer[] result);
    }

    private String[] getSelectedFormsArray() {
        List<String> selectedForms = ((NewFormsAdapter) listAdapter).getSelectedForms();
        String[] selectedFormUuids = new String[selectedForms.size()];
        return selectedForms.toArray(selectedFormUuids);
    }

    private void updateSyncText() {
        SharedPreferences pref = getActivity().getSharedPreferences(Constants.SYNC_PREF, Context.MODE_PRIVATE);
        long lastSyncedTime = pref.getLong(FORMS_METADATA_LAST_SYNCED_TIME, NOT_SYNCED_TIME);
        String lastSyncedMsg = "Not synced yet";
        if(lastSyncedTime != NOT_SYNCED_TIME){
            lastSyncedMsg = "Last synced on: " + DateUtils.getFormattedDateTime(new Date(lastSyncedTime));
        }
        syncText.setText(lastSyncedMsg);
    }
}
