/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import com.muzima.R;
import com.muzima.adapters.forms.FormsWithDataAdapter;
import com.muzima.api.model.FormData;
import com.muzima.controller.FormController;
import com.muzima.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public abstract class FormsWithDataListFragment extends FormsListFragment{
    ActionMode actionMode;
    boolean actionModeActive;

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
                    List<String> selectedFormsUUIDs = ((FormsWithDataAdapter) listAdapter).getSelectedFormsUuids();
                    try {
                        Map<String,List<FormData>> groupedFormData = formController.getFormDataGroupedByPatient(selectedFormsUUIDs);
                        final Map<String,List<FormData>> groupedFormDataWithRegistrationData =
                                formController.deleteFormDataWithNoRelatedCompleteRegistrationFormDataInGroup(groupedFormData);
                        if(!groupedFormDataWithRegistrationData.isEmpty()){
                            onPartialCompleteOfFormDelete(groupedFormDataWithRegistrationData.values());
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setCancelable(true)
                                .setIcon(ThemeUtils.getIconWarning(getContext()))
                                .setTitle(getResources().getString(R.string.general_alert))
                                .setMessage(R.string.warning_registration_form_data_delete)
                                .setPositiveButton(R.string.general_yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        try {
                                            Map<String,List<FormData>> groupedFormDataFailingDeletionValidation =
                                                formController.deleteRegistrationFormDataWithAllRelatedEncountersInGroup(groupedFormDataWithRegistrationData);
                                            if (groupedFormDataFailingDeletionValidation.isEmpty()) {
                                                onCompleteOfFormDelete();
                                            } else {
                                                onPartialCompleteOfFormDelete(groupedFormDataFailingDeletionValidation.values());
                                                Toast.makeText(getActivity(), R.string.info_form_delete_integrity_failure, Toast.LENGTH_LONG).show();
                                            }
                                        } catch (FormController.FormDeleteException | FormController.FormDataFetchException e) {
                                            Log.e(getClass().getSimpleName(),"Could not delete form data ",e);
                                            Toast.makeText(getActivity(),R.string.error_form_data_delete,Toast.LENGTH_LONG).show();
                                        }
                                    }
                                })
                                .setNegativeButton(R.string.general_cancel, null)
                                .show();

                        } else {
                            onCompleteOfFormDelete();
                        }
                    } catch (FormController.FormDataDeleteException e) {
                        Log.e(getClass().getSimpleName(),"Could not delete form data ",e);
                        Toast.makeText(getActivity(),R.string.error_form_data_delete,Toast.LENGTH_LONG).show();
                    } catch (FormController.FormDataFetchException e) {
                        Log.e(getClass().getSimpleName(),"Could not validate form data deletion ",e);
                        Toast.makeText(getActivity(),R.string.error_form_data_delete,Toast.LENGTH_LONG).show();
                    }
            }
            return false;
        }

        private void onCompleteOfFormDelete() {
            endActionMode();
            reloadData();
            ((FormsWithDataAdapter) listAdapter).clearSelectedFormsUuid();
            listAdapter.notifyDataSetChanged();
            unselectAllItems();
            Toast.makeText(getActivity(), R.string.info_form_delete_success, Toast.LENGTH_SHORT).show();
        }
        private void onPartialCompleteOfFormDelete(Collection<List<FormData>> remnantFormData){
            reloadData();
            Collection<String> uuids =new ArrayList<>();
            for(List<FormData> formDataList:remnantFormData) {
                for(FormData formData:formDataList){
                    uuids.add(formData.getUuid());
                }
            }
            int selectedFormsSize = ((FormsWithDataAdapter) listAdapter).getSelectedFormsUuids().size();
            int remnantFormsSize = uuids.size();
            int deletedFormsCount = selectedFormsSize - remnantFormsSize;
            if(deletedFormsCount > 0) {
                Toast.makeText(getActivity(), getActivity().getString(R.string.info_form_data_delete, deletedFormsCount), Toast.LENGTH_SHORT).show();
                reloadData();
                ((FormsWithDataAdapter) listAdapter).retainFromSelectedFormsUuid(uuids);
                actionMode.setTitle(String.valueOf(remnantFormsSize));
            }
        }

        public void endActionMode() {
            if (actionMode != null) {
                actionMode.finish();
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            ((FormsWithDataAdapter) listAdapter).clearSelectedFormsUuid();
            unselectAllItems(list);
        }
    }
}
