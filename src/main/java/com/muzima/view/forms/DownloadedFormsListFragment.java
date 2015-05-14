/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
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
import com.muzima.model.DownloadedForm;

import java.util.ArrayList;
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
            actionMode.setTitle(String.valueOf(getSelectedForms().size()));
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (!actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionModeActive = true;
        }
        int numOfSelectedForms = getSelectedForms().size();
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
                    List<DownloadedForm> selectedForms = getSelectedForms();
                    List<String> selectedFormsUUIDs = getFormUUIDs(selectedForms);
                    try {
                        List<String> formTemplatesWithAssociatedFormData =
                                formTemplatesWithAssociatedFormData(selectedFormsUUIDs);
                        if (formTemplatesWithAssociatedFormData.isEmpty()) {
                            formController.deleteFormTemplatesByUUID(selectedFormsUUIDs);
                            onCompleteOfFormDelete();
                        } else {
                            Toast.makeText(getActivity(), getCommaSeparatedFormNames(selectedForms, formTemplatesWithAssociatedFormData)
                                    + " contains some associated forms with it. Please complete and sync those forms before deleting them.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (FormController.FormDeleteException e) {
                        Log.e(TAG, "Error while deleting forms", e);
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

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            ((DownloadedFormsAdapter) listAdapter).clearSelectedForms();
        }
    }

    public void endActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }
    private String getCommaSeparatedFormNames(List<DownloadedForm> selectedForms, List<String> formUUIDs) {
        StringBuilder commaSeparatedFormNames = new StringBuilder();
        for (DownloadedForm selectedForm : selectedForms) {
            if (formUUIDs.contains(selectedForm.getFormUuid())) {
                commaSeparatedFormNames.append(selectedForm.getName()).append(", ");
            }
        }
        return commaSeparatedFormNames.toString();
    }

    private List<String> getFormUUIDs(List<DownloadedForm> selectedForms) {
        List<String> formUUIDs = new ArrayList<String>();
        for (DownloadedForm selectedForm : selectedForms) {
            formUUIDs.add(selectedForm.getFormUuid());
        }
        return formUUIDs;
    }

    private List<String> formTemplatesWithAssociatedFormData(List<String> selectedFormsUUIDs) {
        List<String> formsWithAssociatedData = new ArrayList<String>();
        for (String selectedFormsUUID : selectedFormsUUIDs) {
            try {
                if (!formController.getUnUploadedFormData(selectedFormsUUID).isEmpty()) {
                    formsWithAssociatedData.add(selectedFormsUUID);
                }
            } catch (FormController.FormDataFetchException e) {
                Log.e(TAG, "Error while fetching FormData", e);
            }
        }
        return formsWithAssociatedData;
    }

    private List<DownloadedForm> getSelectedForms() {
        List<DownloadedForm> formUUIDs = new ArrayList<DownloadedForm>();
        SparseBooleanArray checkedItemPositions = list.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.valueAt(i)) {
                formUUIDs.add(((DownloadedForm) list.getItemAtPosition(checkedItemPositions.keyAt(i))));
            }
        }
        return formUUIDs;
    }
}
