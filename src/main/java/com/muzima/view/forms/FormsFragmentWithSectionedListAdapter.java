package com.muzima.view.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;
import com.muzima.adapters.forms.SectionedFormsAdapter;
import com.muzima.controller.FormController;

import java.util.List;

public abstract class FormsFragmentWithSectionedListAdapter extends FormsListFragment{

    protected ActionMode actionMode;
    protected boolean actionModeActive;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ((SectionedFormsAdapter)listAdapter).setListView(list);
        return view;
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
                    List<String> selectedFormsUUIDs = ((SectionedFormsAdapter) listAdapter).getSelectedFormsUuid();
                    try {
                        formController.deleteCompleteAndIncompleteForms(selectedFormsUUIDs);
                        onCompleteOfFormDelete();
                        ((SectionedFormsAdapter) listAdapter).clearSelectedFormsUuid();
                    } catch (FormController.FormDeleteException e) {
                    }
            }
            return false;
        }

        private void onCompleteOfFormDelete() {
            endActionMode();
            listAdapter.reloadData();
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
        }
    }


}
