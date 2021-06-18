package com.muzima.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.cohort.CohortFilterAdapter;
import com.muzima.adapters.patients.AllPatientsAdapter;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.CohortFilter;
import com.muzima.api.model.Patient;
import com.muzima.controller.CohortController;
import com.muzima.controller.FormController;
import com.muzima.controller.PatientController;
import com.muzima.model.events.CohortFilterActionEvent;
import com.muzima.model.events.ShowCohortFilterEvent;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.tasks.LoadPatientsListService;
import com.muzima.utils.Fonts;
import com.muzima.view.ClientSummaryActivity;
import com.muzima.view.patients.PatientsListActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardHomeFragment extends Fragment implements LoadPatientsListService.PatientsListLoadedCallback,
        AllPatientsAdapter.OnPatientClickedListener {
    private static final String TAG = "DashboardHomeFragment";
    private TextView incompleteFormsTextView;
    private TextView completeFormsTextView;
    private View searchPatientEditText;
    private View searchByBarCode;
    private View searchByFingerprint;
    private View searchByServer;
    private View searchBySmartCard;
    private View fragmentContentContainer;
    private View favouriteListView;
    private View childContainer;
    private TextView providerNameTextView;
    private RecyclerView listView;
    private View noDataView;
    private FloatingActionButton fabSearchButton;
    private FrameLayout progressBarContainer;
    private TextView noDataTipTextView;
    private TextView noDataMsgTextView;
    private AllPatientsAdapter allPatientsAdapter;
    private ProgressBar progressBar;
    private TextView filterLabelTextView;
    private List<Patient> patients = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard_home, container, false);
        initializeResources(view);
        setupNoDataView();
        setUpFormsCount();
        loadAllPatients();
        return view;
    }

    private void loadAllPatients() {
        ((MuzimaApplication) getActivity().getApplicationContext()).getExecutorService()
                .execute(new LoadPatientsListService(getActivity().getApplicationContext(), this));
    }

    @Override
    public void onPatientsLoaded(final List<Patient> patientsList) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                patients.addAll(patientsList);
                allPatientsAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                if (patients.isEmpty())
                    noDataView.setVisibility(View.VISIBLE);
                else
                    listView.setVisibility(View.VISIBLE);

            }
        });
    }

    @Override
    public void onPatientClicked(int position) {
        Patient patient = patients.get(position);
        Intent intent = new Intent(getActivity().getApplicationContext(), ClientSummaryActivity.class);
        intent.putExtra(ClientSummaryActivity.PATIENT_UUID, patient.getUuid());
        startActivity(intent);
    }

    private void setUpFormsCount() {
        try {
            long incompleteForms = ((MuzimaApplication) getActivity().getApplicationContext()).getFormController().countAllIncompleteForms();
            long completeForms = ((MuzimaApplication) getActivity().getApplicationContext()).getFormController().countAllCompleteForms();
            incompleteFormsTextView.setText(String.valueOf(incompleteForms));
            completeFormsTextView.setText(String.valueOf(completeForms));
        } catch (FormController.FormFetchException e) {
            e.printStackTrace();
        }
    }

    private void initializeResources(View view) {
        noDataView = view.findViewById(R.id.no_data_layout);
        listView = view.findViewById(R.id.list);
        noDataTipTextView = view.findViewById(R.id.no_data_tip);
        noDataMsgTextView = view.findViewById(R.id.no_data_msg);
        incompleteFormsTextView = view.findViewById(R.id.dashboard_forms_incomplete_forms_count_view);
        completeFormsTextView = view.findViewById(R.id.dashboard_forms_complete_forms_count_view);
        searchPatientEditText = view.findViewById(R.id.dashboard_main_patient_search_view);
        searchByBarCode = view.findViewById(R.id.search_barcode_view);
        searchByFingerprint = view.findViewById(R.id.search_fingerprint);
        searchByServer = view.findViewById(R.id.search_server_view);
        searchBySmartCard = view.findViewById(R.id.search_smart_card_view);
        favouriteListView = view.findViewById(R.id.favourite_list_container);
        progressBarContainer = view.findViewById(R.id.progressbarContainer);
        fabSearchButton = view.findViewById(R.id.fab_search);
        progressBar = view.findViewById(R.id.patient_loader_progress_bar);
        providerNameTextView = view.findViewById(R.id.dashboard_home_welcome_message_text_view);
        fragmentContentContainer = view.findViewById(R.id.dashboard_home_fragment_container);
        filterLabelTextView = view.findViewById(R.id.dashboard_home_filter_text_view);
        childContainer = view.findViewById(R.id.dashboard_home_fragment_child_container);
        allPatientsAdapter = new AllPatientsAdapter(getActivity().getApplicationContext(), patients, this, getCurrentGPSLocation());
        listView.setAdapter(allPatientsAdapter);
        listView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        listView.setVisibility(View.GONE);

        favouriteListView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBus.getDefault().post(new ShowCohortFilterEvent());
                childContainer.setBackgroundColor(getResources().getColor(R.color.hint_text_grey_opaque));
                fabSearchButton.hide();
            }
        });

        searchPatientEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity().getApplicationContext(), PatientsListActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });

        fabSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity().getApplicationContext(), PatientsListActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });

        providerNameTextView.setText(String.format(Locale.getDefault(), "%s, %s",
                getResources().getString(R.string.general_hello_greeting),
                ((MuzimaApplication) getActivity().getApplicationContext()).getAuthenticatedUser().getUsername()));

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
    public void cohortFilterEvent(CohortFilterActionEvent event) {
        if (event.getFilter() == null && event.isNoSelectionEvent()) {
            try {
                patients.addAll(((MuzimaApplication) getActivity().getApplicationContext()).getPatientController()
                        .getAllPatients());
                allPatientsAdapter.notifyDataSetChanged();
            } catch (PatientController.PatientLoadException ex) {
                ex.printStackTrace();
            }
        } else if (event.getFilter() != null && event.isNoSelectionEvent() == false) {
            try {
                List<Patient> patientList = ((MuzimaApplication) getActivity().getApplicationContext()).getPatientController()
                        .getPatientsForCohorts(new String[]{event.getFilter().getUuid()});
                patients.clear();
                patients.addAll(patientList);
                allPatientsAdapter.notifyDataSetChanged();
                filterLabelTextView.setText(event.getFilter().getName());
            } catch (PatientController.PatientLoadException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void setupNoDataView() {
        noDataMsgTextView.setText(getResources().getText(R.string.info_no_client_available));
        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(getActivity().getApplicationContext()));
        noDataTipTextView.setTypeface(Fonts.roboto_medium(getActivity().getApplicationContext()));
    }

    private MuzimaGPSLocation getCurrentGPSLocation() {
        MuzimaGPSLocationService muzimaLocationService = ((MuzimaApplication) getActivity().getApplicationContext())
                .getMuzimaGPSLocationService();
        return muzimaLocationService.getLastKnownGPSLocation();
    }
}
