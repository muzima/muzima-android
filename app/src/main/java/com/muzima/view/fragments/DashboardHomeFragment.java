package com.muzima.view.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.patients.AllPatientsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.model.CohortFilter;
import com.muzima.model.events.BottomSheetToggleEvent;
import com.muzima.model.events.CloseBottomSheetEvent;
import com.muzima.model.events.CohortFilterActionEvent;
import com.muzima.model.events.ShowCohortFilterEvent;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.tasks.FilterPatientsListTask;
import com.muzima.tasks.LoadPatientsListService;
import com.muzima.utils.FormUtils;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.utils.ThemeUtils;
import com.muzima.utils.barcode.BarCodeScannerIntentIntegrator;
import com.muzima.utils.smartcard.SmartCardIntentIntegrator;
import com.muzima.view.ClientSummaryActivity;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.forms.CompletedFormsListActivity;
import com.muzima.view.forms.IncompleteFormsListActivity;
import com.muzima.view.forms.RegistrationFormsActivity;
import com.muzima.view.patients.PatientsListActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardHomeFragment extends Fragment implements LoadPatientsListService.PatientsListLoadedCallback,
        AllPatientsAdapter.OnPatientClickedListener {
    private TextView incompleteFormsTextView;
    private TextView completeFormsTextView;
    private View incompleteFormsView;
    private View completeFormsView;
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
    private AppBarLayout appBarLayout;
    private TextView filterLabelTextView;
    private ProgressBar filterProgressBar;
    private List<Patient> patients = new ArrayList<>();
    private boolean bottomSheetFilterVisible;

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
        if (getActivity() == null) return;
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
        if (bottomSheetFilterVisible) {
            closeBottomSheet();
        } else {
            Patient patient = patients.get(position);
            Intent intent = new Intent(getActivity().getApplicationContext(), ClientSummaryActivity.class);
            intent.putExtra(ClientSummaryActivity.PATIENT_UUID, patient.getUuid());
            intent.putExtra(ClientSummaryActivity.CALLING_ACTIVITY, MainDashboardActivity.class.getSimpleName());
            startActivity(intent);
        }
    }

    private void setUpFormsCount() {
        try {
            long incompleteForms = ((MuzimaApplication) getActivity().getApplicationContext()).getFormController().countAllIncompleteForms();
            long completeForms = ((MuzimaApplication) getActivity().getApplicationContext()).getFormController().countAllCompleteForms();
            incompleteFormsTextView.setText(String.valueOf(incompleteForms));
            applyFormsCount(incompleteFormsTextView, incompleteForms);
            applyFormsCount(completeFormsTextView, completeForms);
        } catch (FormController.FormFetchException e) {
            e.printStackTrace();
        }
    }

    private void applyFormsCount(TextView textView, long count) {
        if (count < 10)
            textView.setText(String.format(Locale.getDefault(), "%d", count));
        else
            textView.setText(String.valueOf(count));
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
        appBarLayout = view.findViewById(R.id.dashboard_home_app_bar);
        incompleteFormsView = view.findViewById(R.id.dashboard_forms_incomplete_forms_view);
        completeFormsView = view.findViewById(R.id.dashboard_forms_complete_forms_view);
        filterProgressBar = view.findViewById(R.id.patient_list_filtering_progress_bar);
        allPatientsAdapter = new AllPatientsAdapter(getActivity().getApplicationContext(), patients, this, getCurrentGPSLocation());
        listView.setAdapter(allPatientsAdapter);
        listView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        listView.setVisibility(View.GONE);
        filterProgressBar.setVisibility(View.GONE);

        incompleteFormsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetFilterVisible) {
                    closeBottomSheet();
                } else {
                    launchFormDataList(true);
                }
            }
        });

        completeFormsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetFilterVisible) {
                    closeBottomSheet();
                } else {
                    launchFormDataList(false);
                }
            }
        });

        favouriteListView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetFilterVisible) {
                    closeBottomSheet();
                } else {
                    EventBus.getDefault().post(new ShowCohortFilterEvent());
                    bottomSheetFilterVisible = true;
                    childContainer.setBackgroundColor(getResources().getColor(R.color.hint_text_grey_opaque));
                    fabSearchButton.hide();
                }
            }
        });

        searchPatientEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPatientsListIfNecessary();
            }
        });

        childContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBus.getDefault().post(new CloseBottomSheetEvent());
            }
        });

        searchBySmartCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readSmartCard();
            }
        });

        searchByBarCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                invokeBarcodeScan();
            }
        });

        fabSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callRegisterPatientConfirmationDialog();
            }
        });

        providerNameTextView.setText(String.format(Locale.getDefault(), "%s, %s",
                getResources().getString(R.string.general_hello_greeting),
                ((MuzimaApplication) getActivity().getApplicationContext()).getAuthenticatedUser().getUsername()));

    }

    private void showRegistrationFormsMissingAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        AlertDialog dialog = builder.setCancelable(false)
                .setIcon(ThemeUtils.getIconWarning(getActivity().getApplicationContext()))
                .setTitle(getResources().getString(R.string.general_alert))
                .setMessage(getResources().getString(R.string.general_registration_form_missing_message))
                .setPositiveButton(R.string.general_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();
        dialog.show();
    }

    private void launchPatientsListIfNecessary() {
        if (bottomSheetFilterVisible) {
            closeBottomSheet();
        } else {
            Intent intent = new Intent(getActivity().getApplicationContext(), PatientsListActivity.class);
            intent.putExtra(PatientsListActivity.QUICK_SEARCH, true);
            startActivity(intent);
            getActivity().finish();
        }
    }

    // Confirmation dialog for confirming if the patient have an existing ID
    private void callRegisterPatientConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(true)
                .setIcon(ThemeUtils.getIconWarning(getActivity().getApplicationContext()))
                .setTitle(getResources().getString(R.string.title_logout_confirm))
                .setMessage(getResources().getString(R.string.confirm_patient_id_exists))
                .setPositiveButton(R.string.general_yes, launchPatientsList())
                .setNegativeButton(R.string.general_no, launchClientRegistrationFormIfPossible()).create().show();
    }

    private Dialog.OnClickListener launchPatientsList() {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                launchPatientsListIfNecessary();
            }
        };
    }

    private Dialog.OnClickListener launchClientRegistrationFormIfPossible() {
        if (FormUtils.getRegistrationForms(((MuzimaApplication) getActivity().getApplicationContext()).getFormController()).isEmpty()) {
            return new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showRegistrationFormsMissingAlert();
                }
            };
        } else {
            return new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(getActivity().getApplicationContext(), RegistrationFormsActivity.class));
                }
            };
        }
    }

    private void invokeBarcodeScan() {
        BarCodeScannerIntentIntegrator scanIntegrator = new BarCodeScannerIntentIntegrator(getActivity());
        scanIntegrator.initiateScan();
    }

    private void readSmartCard() {
        if (getActivity() == null) return;
        SmartCardIntentIntegrator SHRIntegrator = new SmartCardIntentIntegrator(getActivity());
        SHRIntegrator.initiateCardRead();
        Toast.makeText(getActivity().getApplicationContext(), "Opening Card Reader", Toast.LENGTH_LONG).show();
    }

    private void launchFormDataList(boolean incompleteForms) {
        Intent intent;
        if (incompleteForms) {
            intent = new Intent(getActivity().getApplicationContext(), IncompleteFormsListActivity.class);
        } else {
            intent = new Intent(getActivity().getApplicationContext(), CompletedFormsListActivity.class);
        }
        startActivity(intent);
        getActivity().finish();

    }

    private void closeBottomSheet() {
        EventBus.getDefault().post(new CloseBottomSheetEvent());
        bottomSheetFilterVisible = false;
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
    public void bottomNavigationToggleEvent(BottomSheetToggleEvent event) {
        if (event.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            childContainer.setVisibility(View.VISIBLE);
            appBarLayout.setBackgroundColor(getResources().getColor(R.color.hint_text_grey_opaque));
        } else if (event.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            childContainer.setVisibility(View.GONE);
            if (MuzimaPreferences.getIsLightModeThemeSelectedPreference(getActivity().getApplicationContext()))
                appBarLayout.setBackgroundColor(getResources().getColor(R.color.primary_white));
            else
                appBarLayout.setBackgroundColor(getResources().getColor(R.color.primary_black));

        }
    }

    @Subscribe
    public void cohortFilterEvent(final CohortFilterActionEvent event) {
        Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.general_update_patient_list_message), Toast.LENGTH_LONG).show();
        bottomSheetFilterVisible = false;
        applySelectedFilters(event);
        filterProgressBar.setVisibility(View.VISIBLE);
        ((MuzimaApplication) getActivity().getApplicationContext()).getExecutorService().execute(
                new FilterPatientsListTask(getActivity().getApplicationContext(), event, new FilterPatientsListTask.PatientsListFilterCallback() {
                    @Override
                    public void onPatientsFiltered(final List<Patient> patientList) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                filterProgressBar.setVisibility(View.GONE);
                                patients.clear();
                                patients.addAll(patientList);
                                allPatientsAdapter.notifyDataSetChanged();
                                applySelectedFilters(event);
                                Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.general_patient_list_updated_message), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
        );
    }

    private void applySelectedFilters(CohortFilterActionEvent event) {
        if (event.getFilters().size() == 1)
            filterLabelTextView.setText(event.getFilters().get(0).getCohort().getName());
        else if (event.getFilters().isEmpty())
            filterLabelTextView.setText(getActivity().getResources().getString(R.string.general_all_clients));
        else if(event.getFilters().size() == 1 && event.getFilters().get(0) != null && event.getFilters().get(0).getCohort() == null)
            filterLabelTextView.setText(getActivity().getResources().getString(R.string.general_all_clients));
        else if (event.getFilters().size() > 1) {
            filterLabelTextView.setText(getResources().getString(R.string.general_filtered_list));
        }

        for (CohortFilter filter : event.getFilters()) {
            if (filter.getCohort() == null && filter.isSelected())
                filterLabelTextView.setText(getActivity().getResources().getString(R.string.general_all_clients));

        }
    }

    private void setupNoDataView() {
        noDataMsgTextView.setText(getResources().getText(R.string.info_no_client_available));
    }

    private MuzimaGPSLocation getCurrentGPSLocation() {
        MuzimaGPSLocationService muzimaLocationService = ((MuzimaApplication) getActivity().getApplicationContext())
                .getMuzimaGPSLocationService();
        return muzimaLocationService.getLastKnownGPSLocation();
    }
}
