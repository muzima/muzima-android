package com.muzima.view.fragments.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.FormsRecyclerViewAdapter;
import com.muzima.api.model.Form;
import com.muzima.model.FormItem;
import com.muzima.model.events.DestroyActionModeEvent;
import com.muzima.model.events.FormSearchEvent;
import com.muzima.model.events.FormsActionModeEvent;
import com.muzima.tasks.LoadAllFormsTask;
import com.muzima.tasks.LoadAvailableFormsTask;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class AvailableFormsFragment extends Fragment implements FormsRecyclerViewAdapter.OnFormClickedListener {
    private RecyclerView formsRecyclerView;
    private ProgressBar progressBar;
    private FormsRecyclerViewAdapter recyclerViewAdapter;
    private View filterByStatusView;
    private List<FormItem> formList = new ArrayList<>();
    private List<Form> selectedForms = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forms_list, container, false);
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
        if (event.getPage() == 2)
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initializeResources(view);
        loadData(null);
    }

    private void loadData(String searchTerm) {
        ((MuzimaApplication) getActivity().getApplicationContext()).getExecutorService()
                .execute(new LoadAvailableFormsTask(getActivity().getApplicationContext(), searchTerm, new LoadAllFormsTask.FormsLoadedCallback() {
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
                                recyclerViewAdapter.notifyDataSetChanged();
                                recyclerViewAdapter.setItemsCopy(formList, "Available forms callback");
                            }
                        });
                    }
                }));
    }

    private void initializeResources(View view) {
        formsRecyclerView = view.findViewById(R.id.forms_list_recycler_view);
        filterByStatusView = view.findViewById(R.id.forms_sort_by_status);
        progressBar = view.findViewById(R.id.form_list_progress_bar);
        recyclerViewAdapter = new FormsRecyclerViewAdapter(getActivity().getApplicationContext(), formList, this);
        formsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        formsRecyclerView.setAdapter(recyclerViewAdapter);
    }

    @Override
    public void onFormClicked(int position) {
        FormItem form = formList.get(position);
        form.setSelected(!form.isSelected());
        recyclerViewAdapter.notifyDataSetChanged();
        if (form.isSelected())
            selectedForms.add(form.getForm());
        else
            selectedForms.remove(form.getForm());
        EventBus.getDefault().post(new FormsActionModeEvent(selectedForms));
    }
}
