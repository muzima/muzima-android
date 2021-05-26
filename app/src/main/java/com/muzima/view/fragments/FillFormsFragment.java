package com.muzima.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.ClientSummaryFormsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.model.DownloadedForm;
import com.muzima.tasks.FormsLoaderService;
import com.muzima.view.forms.FormViewIntent;

import java.util.ArrayList;
import java.util.List;

import static com.muzima.view.relationship.RelationshipsListActivity.INDEX_PATIENT;

public class FillFormsFragment extends Fragment implements FormsLoaderService.FormsLoadedCallback, ClientSummaryFormsAdapter.OnFormClickedListener {

    private RecyclerView formsRecyclerView;
    private ClientSummaryFormsAdapter formsAdapter;
    private Patient patient;
    private String patientUuid;
    private List<DownloadedForm> forms = new ArrayList<>();

    public FillFormsFragment(String patientUuid) {
        this.patientUuid = patientUuid;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fill_forms, container, false);
        initializeResources(view);
        loadData();
        return view;
    }

    private void loadData() {
        ((MuzimaApplication) getActivity().getApplicationContext()).getExecutorService()
                .execute(new FormsLoaderService(getActivity().getApplicationContext(), this));
    }

    private void initializeResources(View view) {
        formsRecyclerView = view.findViewById(R.id.fragment_fill_forms_recycler_view);
        formsAdapter = new ClientSummaryFormsAdapter(forms, this);
        formsRecyclerView.setAdapter(formsAdapter);
        formsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity().getApplicationContext(),DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.divider_item_view));
        formsRecyclerView.addItemDecoration(dividerItemDecoration);
        try {
            patient = ((MuzimaApplication) getActivity().getApplicationContext()).getPatientController().getPatientByUuid(patientUuid);
        }catch (PatientController.PatientLoadException ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void onFormsLoaded(final List<DownloadedForm> formList) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                forms.addAll(formList);
                formsAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onFormClickedListener(int position) {
        DownloadedForm form = forms.get(position);

        Intent intent = new FormViewIntent(getActivity(), form, patient , false);
        intent.putExtra(INDEX_PATIENT, patient);
        startActivity(intent);
    }
}
