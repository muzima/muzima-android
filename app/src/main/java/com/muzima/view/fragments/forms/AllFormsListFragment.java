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
import com.muzima.tasks.LoadAllFormsTask;

import java.util.ArrayList;
import java.util.List;

public class AllFormsListFragment extends Fragment implements FormsRecyclerViewAdapter.OnFormClickedListener {
    private RecyclerView formsRecyclerView;
    private ProgressBar progressBar;
    private FormsRecyclerViewAdapter recyclerViewAdapter;
    private View filterByStatusView;
    private List<Form> formList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forms_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initializeResources(view);
        loadData();
    }

    private void loadData() {
        ((MuzimaApplication) getActivity().getApplicationContext()).getExecutorService()
                .execute( new LoadAllFormsTask(getActivity().getApplicationContext(), new LoadAllFormsTask.FormsLoadedCallback() {
                    @Override
                    public void onFormsLoaded(final List<Form> forms) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                formList.addAll(forms);
                                recyclerViewAdapter.notifyDataSetChanged();
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
        formsRecyclerView.setLayoutManager( new LinearLayoutManager(getActivity().getApplicationContext()));
        formsRecyclerView.setAdapter(recyclerViewAdapter);
    }

    @Override
    public void onFormClicked(int position) {
        Form form = formList.get(position);
    }
}
