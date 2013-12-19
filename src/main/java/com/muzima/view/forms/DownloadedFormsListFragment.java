package com.muzima.view.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;
import com.muzima.adapters.forms.DownloadedFormsAdapter;
import com.muzima.controller.FormController;

public class DownloadedFormsListFragment extends FormsListFragment implements AllAvailableFormsListFragment.OnTemplateDownloadComplete {

    private boolean actionModeActive = false;
    private ActionMode actionMode;

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
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (!actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionModeActive = true;
        }         ((DownloadedFormsAdapter) listAdapter).onListItemClick(position);
        int numOfSelectedForms = ((DownloadedFormsAdapter) listAdapter).getSelectedForms().size();
        if (numOfSelectedForms == 0 && actionModeActive) {
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numOfSelectedForms));

    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container){
        return inflater.inflate(R.layout.layout_list, container, false);
    }

    @Override
    public void onTemplateDownloadComplete() {
        listAdapter.reloadData();
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
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
        }
    }

}
