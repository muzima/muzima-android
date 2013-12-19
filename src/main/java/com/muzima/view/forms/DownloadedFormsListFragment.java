package com.muzima.view.forms;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;
import com.muzima.adapters.forms.DownloadedFormsAdapter;
import com.muzima.controller.FormController;

import java.util.List;

public class DownloadedFormsListFragment extends FormsListFragment implements AllAvailableFormsListFragment.OnTemplateDownloadComplete {

    private static final String TAG = "DownloadedFormsListFragment";
    private boolean actionModeActive = false;
    private ActionMode actionMode;
    private AllAvailableFormsListFragment allAvailableFormsCompleteListener;

    public static DownloadedFormsListFragment newInstance(FormController formController) {
        DownloadedFormsListFragment f = new DownloadedFormsListFragment();
        f.formController = formController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        listAdapter = new DownloadedFormsAdapter(getActivity(), R.layout.item_forms_list_selectable, formController);
        noDataMsg = getActivity().getResources().getString(R.string.no_downloaded_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_downloaded_form_tip);

        // this can happen on orientation change
        if (actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionMode.setTitle(String.valueOf(((DownloadedFormsAdapter) listAdapter).getSelectedForms().size()));
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (!actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionModeActive = true;
        }
        ((DownloadedFormsAdapter) listAdapter).onListItemClick(position);
        int numOfSelectedForms = ((DownloadedFormsAdapter) listAdapter).getSelectedForms().size();
        if (numOfSelectedForms == 0 && actionModeActive) {
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numOfSelectedForms));
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.layout_list, container, false);
    }

    @Override
    public void onTemplateDownloadComplete() {
        listAdapter.reloadData();
    }

    public void setAllAvailableFormsCompleteListener(AllAvailableFormsListFragment
                                                             allAvailableFormsCompleteListener) {
        this.allAvailableFormsCompleteListener = allAvailableFormsCompleteListener;
    }

    public final class DeleteFormsActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            getSherlockActivity().getSupportMenuInflater().inflate(R.menu.actionmode_menu_delete, menu);
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
                    List<String> selectedFormsUUIDs = ((DownloadedFormsAdapter) listAdapter).getSelectedForms();
                    try {
                        formController.deleteFormTemplatesByUUID(selectedFormsUUIDs);
                        onCompleteOfFormDelete();
                    } catch (FormController.FormDeleteException e) {
                        Log.e(TAG, "Error while deleting forms");
                    }
            }
            return false;
        }

        private void onCompleteOfFormDelete() {
            endActionMode();
            listAdapter.reloadData();
            allAvailableFormsCompleteListener.reloadData();
            Toast.makeText(getActivity(), "Forms deleted successfully!!", Toast.LENGTH_SHORT).show();
        }

        private void endActionMode() {
            if (actionMode != null) {
                actionMode.finish();
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            ((DownloadedFormsAdapter) listAdapter).clearSelectedForms();
        }
    }

}
