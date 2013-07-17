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
import com.muzima.adapters.NewFormsAdapter;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;

import java.util.List;

public class NewFormsListFragment extends FormsListFragment {
    private static final String TAG = "NewFormsListFragment";

    private ActionMode actionMode;
    private boolean actionModeActive = false;

    public static NewFormsListFragment newInstance(FormController formController, String noDataMsg, String noDataTip) {
        NewFormsListFragment f = new NewFormsListFragment();
        f.noDataMsg = noDataMsg;
        f.noDataTip = noDataTip;
        f.formController = formController;
        f.setRetainInstance(true);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        listAdapter = new NewFormsAdapter(getActivity(), R.layout.item_forms_list, formController);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (actionModeActive == false) {
            actionMode = getSherlockActivity().startActionMode(new NewFormsActionModeCallback());
            actionModeActive = true;
        }
        ((NewFormsAdapter) listAdapter).onListItemClick(position);
        int numOfSelectedForms = ((NewFormsAdapter) listAdapter).getSelectedForms().size();
        actionMode.setTitle(String.valueOf(numOfSelectedForms));
    }

    public final class NewFormsActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            getSherlockActivity().getSupportMenuInflater().inflate(R.menu.form_list_actionmode_menu, menu);
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
                    List<Form> selectedForms = ((NewFormsAdapter) listAdapter).getSelectedForms();
                    formController.downloadFormsTemplate(selectedForms);
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
            ((NewFormsAdapter)listAdapter).clearSelectedForms();
        }
    }
}
