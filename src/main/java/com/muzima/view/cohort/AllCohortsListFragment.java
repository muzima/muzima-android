package com.muzima.view.cohort;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.cohort.AllCohortsAdapter;
import com.muzima.adapters.forms.NewFormsAdapter;
import com.muzima.controller.CohortController;
import com.muzima.search.api.util.StringUtil;
import com.muzima.tasks.forms.DownloadFormTemplateTask;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.forms.NewFormsListFragment;

import static android.os.AsyncTask.Status.PENDING;
import static android.os.AsyncTask.Status.RUNNING;

public class AllCohortsListFragment extends CohortListFragment{

    private ActionMode actionMode;
    private boolean actionModeActive = false;

    public static AllCohortsListFragment newInstance(CohortController cohortController) {
        AllCohortsListFragment f = new AllCohortsListFragment();
        f.cohortController = cohortController;
        f.setRetainInstance(true);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(listAdapter == null){
            listAdapter = new AllCohortsAdapter(getActivity(), R.layout.item_cohorts_list, cohortController);
        }
        noDataMsg = getActivity().getResources().getString(R.string.no_cohorts_available);
        noDataTip = getActivity().getResources().getString(R.string.no_cohorts_available_tip);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    //    @Override
//    public void onDestroy() {
//        if(formTemplateDownloadTask != null){
//            formTemplateDownloadTask.cancel(false);
//        }
//        super.onDestroy();
//    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (!actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new AllCohortsActionModeCallback());
            actionModeActive = true;
        }
    }

    public final class AllCohortsActionModeCallback implements ActionMode.Callback {

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

//                    if (formTemplateDownloadTask != null &&
//                            (formTemplateDownloadTask.getStatus() == PENDING || formTemplateDownloadTask.getStatus() == RUNNING)) {
//                        Toast.makeText(getActivity(), "Already fetching form templates, ignored the request", Toast.LENGTH_SHORT).show();
//                        return true;
//                    }
//                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
//                    formTemplateDownloadTask = new DownloadFormTemplateTask((MuzimaApplication) getActivity().getApplication());
//                    formTemplateDownloadTask.addDownloadListener(NewFormsListFragment.this);
//                    String usernameKey = getResources().getString(R.string.preference_username);
//                    String passwordKey = getResources().getString(R.string.preference_password);
//                    String serverKey = getResources().getString(R.string.preference_server);
//                    String[] credentials = new String[]{settings.getString(usernameKey, StringUtil.EMPTY),
//                            settings.getString(passwordKey, StringUtil.EMPTY),
//                            settings.getString(serverKey, StringUtil.EMPTY)};
//                    formTemplateDownloadTask.execute(credentials, getSelectedFormsArray());
//                    if (NewFormsListFragment.this.actionMode != null) {
//                        NewFormsListFragment.this.actionMode.finish();
//                    }
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            ((AllCohortsAdapter) listAdapter).clearSelectedCohorts();
        }
    }

}
