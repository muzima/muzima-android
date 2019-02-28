/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import com.muzima.R;
import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.adapters.forms.FormsWithDataAdapter;
import com.muzima.controller.FormController;
import com.muzima.view.MuzimaListFragment;

import java.util.List;

import static com.muzima.adapters.ListAdapter.BackgroundListQueryTaskListener;

public abstract class FormsListFragment extends MuzimaListFragment implements BackgroundListQueryTaskListener{

    FormController formController;
    private FrameLayout progressBarContainer;
    private LinearLayout noDataView;
    ActionMode actionMode;
    boolean actionModeActive;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View formsLayout = setupMainView(inflater, container);
        list = formsLayout.findViewById(R.id.list);
        progressBarContainer = formsLayout.findViewById(R.id.progressbarContainer);
        noDataView = formsLayout.findViewById(R.id.no_data_layout);

        setupNoDataView(formsLayout);

        // Todo no need to do this check after all list adapters are implemented
        if (listAdapter != null) {
            list.setAdapter(listAdapter);
            list.setOnItemClickListener(this);
            ((FormsAdapter)listAdapter).setBackgroundListQueryTaskListener(this);
        }
        list.setEmptyView(formsLayout.findViewById(R.id.no_data_layout));

        return formsLayout;
    }

    View setupMainView(LayoutInflater inflater, ViewGroup container){
        return inflater.inflate(R.layout.layout_list, container, false);
    }

    public void tagsChanged() {
        listAdapter.reloadData();
    }

    @Override
    public void onQueryTaskStarted() {
        list.setVisibility(View.INVISIBLE);
        noDataView.setVisibility(View.INVISIBLE);
        progressBarContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onQueryTaskFinish() {
        list.setVisibility(View.VISIBLE);
        progressBarContainer.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onQueryTaskCancelled(){}

    @Override
    public void onQueryTaskCancelled(Object errorDefinition){}

    final class DeleteFormsActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            getActivity().getMenuInflater().inflate(R.menu.actionmode_menu_delete, menu);
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
                    List<String> selectedFormsUUIDs = ((FormsWithDataAdapter) listAdapter).getSelectedFormsUuid();
                    try {
                        formController.deleteCompleteAndIncompleteEncounterFormData(selectedFormsUUIDs);
                        onCompleteOfFormDelete();
                        ((FormsWithDataAdapter) listAdapter).clearSelectedFormsUuid();
                    } catch (FormController.FormDeleteException e) {
                        Log.e(getClass().getSimpleName(),""+e.getMessage());
                    }
            }
            return false;
        }

        private void onCompleteOfFormDelete() {
            endActionMode();
            listAdapter.reloadData();
            Toast.makeText(getActivity(), getActivity().getString(R.string.info_form_delete_success), Toast.LENGTH_SHORT).show();
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
