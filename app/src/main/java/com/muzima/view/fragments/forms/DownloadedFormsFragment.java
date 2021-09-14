/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.fragments.forms;

import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.FormsRecyclerViewAdapter;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;
import com.muzima.model.FormItem;
import com.muzima.model.events.DestroyActionModeEvent;
import com.muzima.model.events.FormSearchEvent;
import com.muzima.tasks.LoadDownloadedFormsTask;
import com.muzima.utils.ViewUtil;
import com.muzima.view.custom.MuzimaRecyclerView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class DownloadedFormsFragment extends Fragment implements FormsRecyclerViewAdapter.OnFormClickedListener {
    private MuzimaRecyclerView formsRecyclerView;
    private ProgressBar progressBar;
    private FormsRecyclerViewAdapter recyclerViewAdapter;
    private View filterStrategyContainer;
    private List<FormItem> formList = new ArrayList<>();
    private List<Form> selectedForms = new ArrayList<>();
    ActionMode actionMode;
    boolean actionModeActive;
    FormController formController;

    public static DownloadedFormsFragment newInstance(FormController formController) {
        DownloadedFormsFragment f = new DownloadedFormsFragment();
        f.formController = formController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (actionModeActive) {
            actionMode = requireActivity().startActionMode(new DownloadedFormsFragment.DeleteFormsActionModeCallback());
            actionMode.setTitle(String.valueOf(selectedForms.size()));
        }
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forms_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initializeResources(view);
        loadData(null);
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            EventBus.getDefault().register(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void formsSearchEvent(FormSearchEvent event) {
        if (event.getPage() == 1)
            searchForms(event.getSearchTerm());
    }

    private void searchForms(String searchTerm) {
        loadData(searchTerm);
    }

    @Subscribe
    public void actionModeClosedEvent(DestroyActionModeEvent event) {
        selectedForms.clear();
        for (FormItem formItem : formList) {
            formItem.setSelected(false);
        }
        recyclerViewAdapter.notifyDataSetChanged();
    }

    private void loadData(String searchTerm) {
        if (getActivity() == null) return;
        ((MuzimaApplication) getActivity().getApplicationContext()).getExecutorService()
                .execute(new LoadDownloadedFormsTask(getActivity().getApplicationContext(), searchTerm, new LoadDownloadedFormsTask.FormsLoadedCallback() {
                    @Override
                    public void onFormsLoaded(final List<Form> forms) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                formList.clear();
                                progressBar.setVisibility(View.GONE);
                                for (Form form : forms) {
                                    formList.add(new FormItem(form, false));
                                }
                                ViewUtil.applyFormsListSorting(getActivity().getApplicationContext(),formList,true);
                                recyclerViewAdapter.notifyDataSetChanged();
                                recyclerViewAdapter.setItemsCopy(formList, "Downloaded forms callback");
                            }
                        });
                    }
                }));
    }

    private void initializeResources(View view) {
        formsRecyclerView = view.findViewById(R.id.forms_list_recycler_view);
        filterStrategyContainer = view.findViewById(R.id.form_fragment_child_container);
        progressBar = view.findViewById(R.id.form_list_progress_bar);
        recyclerViewAdapter = new FormsRecyclerViewAdapter(getActivity().getApplicationContext(), formList, this);
        formsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        formsRecyclerView.setAdapter(recyclerViewAdapter);
        formsRecyclerView.setNoDataLayout(view.findViewById(R.id.no_data_layout),
                getString(R.string.info_forms_unavailable),
                getString(R.string.hint_form_download));
        filterStrategyContainer.setVisibility(View.GONE);
    }


    //TODO: This needs to delete
    @Override
    public void onFormClicked(int position) {
        FormItem form = formList.get(position);
        form.setSelected(!form.isSelected());
        recyclerViewAdapter.notifyDataSetChanged();
        if (form.isSelected())
            selectedForms.add(form.getForm());
        else
            selectedForms.remove(form.getForm());

        if (!actionModeActive) {
            actionMode = getActivity().startActionMode(new DownloadedFormsFragment.DeleteFormsActionModeCallback());
            actionModeActive = true;
        }
        int numOfSelectedForms = selectedForms.size();
        if (numOfSelectedForms == 0 && actionModeActive) {
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numOfSelectedForms));
    }

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
                    List<String> selectedFormsUUIDs = new ArrayList<>();
                    for (Form selectedForm : selectedForms) {
                        selectedFormsUUIDs.add(selectedForm.getUuid());
                    }

                    try {
                        List<String> formTemplatesWithAssociatedFormData =
                                formTemplatesWithAssociatedFormData(selectedFormsUUIDs);
                        if (formTemplatesWithAssociatedFormData.isEmpty()) {
                            formController.deleteFormTemplatesByUUID(selectedFormsUUIDs);
                            recyclerViewAdapter.notifyDataSetChanged();
                            endActionMode();
                            Toast.makeText(getActivity(), getActivity().getString(R.string.info_form_delete_success), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(),
                                    getActivity().getString(R.string.warning_forms_complete_and_sync,getCommaSeparatedFormNames(selectedForms, formTemplatesWithAssociatedFormData)),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (FormController.FormDeleteException e) {
                        Log.e(getClass().getSimpleName(), "Error while deleting forms", e);
                    }
            }
            return false;
        }

        private List<String> formTemplatesWithAssociatedFormData(List<String> selectedFormsUUIDs) {
            List<String> formsWithAssociatedData = new ArrayList<>();
            for (String selectedFormsUUID : selectedFormsUUIDs) {
                try {
                    if (!formController.getNonUploadedFormData(selectedFormsUUID).isEmpty()) {
                        formsWithAssociatedData.add(selectedFormsUUID);
                    }
                } catch (FormController.FormDataFetchException e) {
                    Log.e(getClass().getSimpleName(), "Error while fetching FormData", e);
                }
            }
            return formsWithAssociatedData;
        }

        private String getCommaSeparatedFormNames(List<Form> selectedForms, List<String> formUUIDs) {
            StringBuilder commaSeparatedFormNames = new StringBuilder();
            for (Form selectedForm : selectedForms) {
                if (formUUIDs.contains(selectedForm.getUuid())) {
                    commaSeparatedFormNames.append(selectedForm.getName()).append(", ");
                }
            }
            return commaSeparatedFormNames.toString();
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            selectedForms.clear();
            for (FormItem formItem : formList) {
                formItem.setSelected(false);
            }
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    private void endActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }
}
