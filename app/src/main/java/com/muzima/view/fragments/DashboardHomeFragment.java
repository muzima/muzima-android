package com.muzima.view.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.patients.PatientsLocalSearchAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.controller.PatientController;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.utils.Fonts;
import com.muzima.view.patients.PatientSummaryActivity;
import com.muzima.view.patients.PatientsListActivity;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class DashboardHomeFragment extends Fragment implements AdapterView.OnItemClickListener,
        ListAdapter.BackgroundListQueryTaskListener{

    private TextView incompleteFormsTextView;
    private TextView completeFormsTextView;
    private EditText searchPatientEditText;
    private View searchByBarCode;
    private View searchByFingerprint;
    private View searchByServer;
    private View searchBySmartCard;
    private View favouriteListView;
    private ListView listView;
    private View noDataView;
    private FloatingActionButton fabSearchButton;
    private FrameLayout progressBarContainer;
    private PatientsLocalSearchAdapter patientAdapter;
    private TextView noDataTipTextView;
    private TextView noDataMsgTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard_home, container, false);

        initializeResources(view);
        setupNoDataView();
        setupListView();
        setUpFormsCount();
        return view;
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
        searchPatientEditText = view.findViewById(R.id.dashboard_forms_search_edit_text);
        searchByBarCode = view.findViewById(R.id.search_barcode_view);
        searchByFingerprint = view.findViewById(R.id.search_fingerprint);
        searchByServer = view.findViewById(R.id.search_server_view);
        searchBySmartCard = view.findViewById(R.id.search_smart_card_view);
        favouriteListView = view.findViewById(R.id.favourite_list_container);
        progressBarContainer = view.findViewById(R.id.progressbarContainer);
        fabSearchButton = view.findViewById(R.id.fab_search);

        fabSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity().getApplicationContext(),PatientsListActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });
    }

    private void setupNoDataView() {
        try {

            int localPatientsCount = ((MuzimaApplication) getActivity().getApplicationContext())
                    .getPatientController()
                    .countAllPatients();
            if (localPatientsCount == 0)
                noDataMsgTextView.setText(getResources().getText(R.string.info_no_client_available_locally));
            else
                noDataMsgTextView.setText(getResources().getText(R.string.info_client_local_search_not_found));

            noDataTipTextView.setText(R.string.hint_client_local_search);

            noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(getActivity().getApplicationContext()));
            noDataTipTextView.setTypeface(Fonts.roboto_medium(getActivity().getApplicationContext()));
        } catch (PatientController.PatientLoadException ex) {
            Toast.makeText(getActivity().getApplicationContext(), R.string.error_patient_search, Toast.LENGTH_LONG).show();
        }
    }

    private void setupListView() {
        patientAdapter = new PatientsLocalSearchAdapter(getActivity().getApplicationContext(), R.layout.layout_list,
                ((MuzimaApplication) getActivity().getApplicationContext()).getPatientController(), null, getCurrentGPSLocation());

        patientAdapter.setBackgroundListQueryTaskListener(this);
        listView.setAdapter(patientAdapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        patientAdapter.cancelBackgroundTask();
        Patient patient = patientAdapter.getItem(position);
        Intent intent = new Intent(getActivity().getApplicationContext(), PatientSummaryActivity.class);

        intent.putExtra(PatientSummaryActivity.PATIENT, patient);
        startActivity(intent);
    }

    @Override
    public void onQueryTaskStarted() {
        listView.setVisibility(INVISIBLE);
        noDataView.setVisibility(INVISIBLE);
        listView.setEmptyView(progressBarContainer);
        progressBarContainer.setVisibility(VISIBLE);
    }

    @Override
    public void onQueryTaskFinish() {
        listView.setVisibility(VISIBLE);
        listView.setEmptyView(noDataView);
        progressBarContainer.setVisibility(INVISIBLE);
    }

    @Override
    public void onQueryTaskCancelled() {
        Log.e(getClass().getSimpleName(), "Cancelled...");
    }

    @Override
    public void onQueryTaskCancelled(Object errorDefinition) {
        Log.e(getClass().getSimpleName(), "Cancelled...");
    }

    private MuzimaGPSLocation getCurrentGPSLocation() {
        MuzimaGPSLocationService muzimaLocationService = ((MuzimaApplication) getActivity().getApplicationContext())
                .getMuzimaGPSLocationService();
        return muzimaLocationService.getLastKnownGPSLocation();
    }
}
