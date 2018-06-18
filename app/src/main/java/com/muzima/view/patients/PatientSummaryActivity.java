/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.patients;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.patients.PatientAdapterHelper;
import com.muzima.api.model.Location;
import com.muzima.api.model.Patient;
import com.muzima.api.model.SmartCardRecord;
import com.muzima.api.model.User;
import com.muzima.api.service.SmartCardRecordService;
import com.muzima.controller.EncounterController;
import com.muzima.controller.FormController;
import com.muzima.controller.NotificationController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.SmartCardController;
import com.muzima.model.shr.kenyaemr.Addendum.Identifier;
import com.muzima.model.shr.kenyaemr.Addendum.WriteResponse;
import com.muzima.model.shr.kenyaemr.InternalPatientId;
import com.muzima.model.shr.kenyaemr.KenyaEmrSHRModel;
import com.muzima.service.JSONInputOutputToDisk;
import com.muzima.utils.Constants;
import com.muzima.utils.LocationUtils;
import com.muzima.utils.StringUtils;
import com.muzima.utils.smartcard.KenyaEmrSHRMapper;
import com.muzima.utils.smartcard.SmartCardIntentIntegrator;
import com.muzima.utils.smartcard.SmartCardIntentResult;
import com.muzima.view.BaseActivity;
import com.muzima.view.SHRObservationsDataActivity;
import com.muzima.view.encounters.EncountersActivity;
import com.muzima.view.forms.PatientFormsActivity;
import com.muzima.view.notifications.PatientNotificationActivity;
import com.muzima.view.observations.ObservationsActivity;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.muzima.utils.DateUtils.getFormattedDate;
import static com.muzima.utils.smartcard.SmartCardIntentIntegrator.SMARTCARD_READ_REQUEST_CODE;
import static com.muzima.utils.smartcard.SmartCardIntentIntegrator.SMARTCARD_WRITE_REQUEST_CODE;

public class PatientSummaryActivity extends BaseActivity {
    private static final String TAG = "PatientSummaryActivity";
    public static final String PATIENT = "patient";

    private AlertDialog writeShrDataOptionDialog;

    private BackgroundQueryTask mBackgroundQueryTask;

    private Patient patient;
    private ImageView imageView;
    private Boolean isRegisteredOnShr;

    private TextView searchDialogTextView;
    private Button yesOptionShrSearchButton;
    private Button noOptionShrSearchButton;

    private SmartCardRecord smartCardRecord;
    private SmartCardRecordService smartCardRecordService;
    private SmartCardController smartCardController;
    private MuzimaApplication muzimaApplication;
    private Location defaultLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_summary);

        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            patient = (Patient) intentExtras.getSerializable(PATIENT);
            isRegisteredOnShr = patient.getIdentifier(Constants.Shr.KenyaEmr.PersonIdentifierType.CARD_SERIAL_NUMBER.name) == null;

            SmartCardController smartCardController = ((MuzimaApplication) getApplicationContext()).getSmartCardController();
            try {
                isRegisteredOnShr = smartCardController.getSmartCardRecordByPersonUuid(patient.getUuid()) != null;
            } catch (SmartCardController.SmartCardRecordFetchException e) {
                Log.e(TAG, "Error while retrieving smartcard record",e);
            }
        }

        try {
            setupPatientMetadata();
            notifyOfIdChange();
        } catch (PatientController.PatientLoadException e) {
            Toast.makeText(this, R.string.error_patient_fetch, Toast.LENGTH_SHORT).show();
            finish();
        }
        muzimaApplication = (MuzimaApplication) getApplicationContext();

        try {
            smartCardRecordService = muzimaApplication.getMuzimaContext().getSmartCardRecordService();
            smartCardController = new SmartCardController(smartCardRecordService);

        } catch (IOException e) {
            e.printStackTrace();
        }
        imageView = (ImageView) findViewById(R.id.sync_status_imageview);
        if (isRegisteredOnShr) {
            prepareWriteToCardOptionDialog(getApplicationContext());
        } else {
            prepareNonShrWriteToCardOptionDialog(getApplicationContext());
        }
    }

    private void notifyOfIdChange() {
        final JSONInputOutputToDisk jsonInputOutputToDisk = new JSONInputOutputToDisk(getApplication());
        List list = null;
        try {
            list = jsonInputOutputToDisk.readList();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown when reading to phone disk", e);
        }
        if (list.size() == 0) {
            return;
        }

        final String patientIdentifier = patient.getIdentifier();
        if (list.contains(patientIdentifier)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true)
                    .setIcon(getResources().getDrawable(R.drawable.ic_warning))
                    .setTitle(getString(R.string.general_notice))
                    .setMessage(getString(R.string.info_client_identifier_change))
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            patient.removeIdentifier(Constants.LOCAL_PATIENT);
                            try {
                                jsonInputOutputToDisk.remove(patientIdentifier);
                            } catch (IOException e) {
                                Log.e(TAG, "Error occurred while saving patient which has local identifier removed!", e);
                            }
                        }
                    }).create().show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        executeBackgroundTask();
    }

    @Override
    protected void onStop() {
        if (mBackgroundQueryTask != null) {
            mBackgroundQueryTask.cancel(true);
        }
        super.onStop();
    }

    private void setupPatientMetadata() throws PatientController.PatientLoadException {

        TextView patientName = (TextView) findViewById(R.id.patientName);
        patientName.setText(PatientAdapterHelper.getPatientFormattedName(patient));

        ImageView genderIcon = (ImageView) findViewById(R.id.genderImg);
        int genderDrawable = patient.getGender().equalsIgnoreCase("M") ? R.drawable.ic_male : R.drawable.ic_female;
        genderIcon.setImageDrawable(getResources().getDrawable(genderDrawable));

        TextView dob = (TextView) findViewById(R.id.dob);
        dob.setText("DOB: " + getFormattedDate(patient.getBirthdate()));

        TextView patientIdentifier = (TextView) findViewById(R.id.patientIdentifier);
        patientIdentifier.setText(patient.getIdentifier());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.client_summary, menu);
        MenuItem shrCardMenuItem = menu.getItem(0);
        if (isRegisteredOnShr) {
            shrCardMenuItem.setIcon(R.drawable.ic_action_shr_card);
        } else {
            shrCardMenuItem.setVisible(true);
            shrCardMenuItem.setIcon(R.drawable.ic_action_no_shr_card);
        }
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.shr_client_summary:
                //todo write card workspace.
                if (isRegisteredOnShr) {
                    Log.e("TAG", "is Patient shr");
                    prepareWriteToCardOptionDialog(getApplicationContext());
                    writeShrDataOptionDialog.show();
                } else {
                    Log.e("TAG", "is Patient not shr");
                    prepareNonShrWriteToCardOptionDialog(getApplicationContext());
                    writeShrDataOptionDialog.show();
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void initiateShrWriteToCard() {
        SmartCardController smartCardController = ((MuzimaApplication) getApplicationContext()).getSmartCardController();
        SmartCardRecord smartCardRecord = null;
        try {
            KenyaEmrSHRMapper.updateSHRSmartCardRecordForPatient((MuzimaApplication) getApplicationContext(),patient.getUuid());
            smartCardRecord = smartCardController.getSmartCardRecordByPersonUuid(patient.getUuid());
        } catch (SmartCardController.SmartCardRecordFetchException e) {
            Snackbar.make(findViewById(R.id.client_summary_view), R.string.failure_obtain_smartcard_record+e.getMessage(), Snackbar.LENGTH_LONG)
                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_dark))
                    .setAction(R.string.general_retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            initiateShrWriteToCard();
                        }
                    })
                    .show();
            Log.e(TAG, "Could not obtain smartcard record for writing to card", e);
        } catch (KenyaEmrSHRMapper.ShrParseException e) {
            Snackbar.make(findViewById(R.id.client_summary_view), R.string.failure_obtain_smartcard_record+e.getMessage(), Snackbar.LENGTH_LONG)
                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_dark))
                    .setAction(R.string.general_retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            initiateShrWriteToCard();
                        }
                    })
                    .show();
            Log.e(TAG, getString(R.string.failure_updating_smartcard_record_for_writing), e);
        }
        if (smartCardRecord != null) {
            SmartCardIntentIntegrator shrIntegrator = new SmartCardIntentIntegrator(this);
            try {
                shrIntegrator.initiateCardWrite(smartCardRecord.getPlainPayload());
            } catch (IOException e) {
                Log.e(TAG, "Could not write to card", e);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(true)
                        .setMessage(R.string.failure_writing_smartcard + e.getMessage())
                        .show();
            }
            Toast.makeText(getApplicationContext(), getString(R.string.hint_opening_card_reader), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        switch (requestCode) {
            case SMARTCARD_READ_REQUEST_CODE:
                processSmartCardReadResult(requestCode, resultCode, dataIntent);
                break;

            case SMARTCARD_WRITE_REQUEST_CODE:
                SmartCardIntentResult cardWriteIntentResult = null;
                try {
                    cardWriteIntentResult = SmartCardIntentIntegrator.parseActivityResult(requestCode, resultCode, dataIntent);
                    List<String> writeErrors = cardWriteIntentResult.getErrors();

                    if (writeErrors == null) {
                        Snackbar.make(findViewById(R.id.client_summary_view), R.string.success_writing_smartcard, Snackbar.LENGTH_LONG)
                                .show();
                        SmartCardRecord result = cardWriteIntentResult.getSmartCardRecord();

                        try {
                            SmartCardRecord smartCardRecord = smartCardController.getSmartCardRecordByPersonUuid(patient.getUuid());
                            smartCardRecord.setEncryptedPayload(result.getEncryptedPayload());
                            smartCardRecord.setWrittenToCard(true);
                            smartCardRecord.setSyncedToServer(false);
                            smartCardController.updateSmartCardRecord(smartCardRecord);

                            //Deserialize result.getEncryptedPayload() to WriteResponse
                            // get the card serial writeresponse.getcarddetails.
                            WriteResponse writeResponse = new ObjectMapper().readValue(result.getEncryptedPayload(), WriteResponse.class);
                            String cardSerial = null;
                            List<Identifier> addendumIdentifiers = writeResponse.getAddendum().getIdentifiers();
                            for (Identifier id : addendumIdentifiers) {
                                if(id.getIdentifierType().equals("CARD_SERIAL_NUMBER")) {
                                    cardSerial = id.getId();
                                    break;
                                }
                            }
                            KenyaEmrSHRMapper.updatePatientDemographicsWithCardSerialNumberAsIdentifier(muzimaApplication,patient,cardSerial);

                        } catch (SmartCardController.SmartCardRecordFetchException e) {
                            Log.e(TAG,"Could not retrieve SHR from local storage");
                        } catch (SmartCardController.SmartCardRecordSaveException e) {
                            Log.e(TAG,"Could not save SHR from local storage");
                        }

                    } else if (writeErrors != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Snackbar.make(findViewById(R.id.client_summary_view), R.string.failure_writing_smartcard + writeErrors.get(0), Snackbar.LENGTH_LONG)
                                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_dark, null))
                                    .setAction(R.string.general_retry, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            initiateShrWriteToCard();
                                        }
                                    })
                                    .show();
                        } else {

                            Snackbar.make(findViewById(R.id.client_summary_view), R.string.failure_writing_smartcard + writeErrors.get(0), Snackbar.LENGTH_LONG)
                                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_dark))
                                    .setAction(R.string.general_retry, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            initiateShrWriteToCard();
                                        }
                                    })
                                    .show();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


                writeShrDataOptionDialog.dismiss();
                writeShrDataOptionDialog.cancel();
                break;
        }
    }

    public void showForms(View v) {
        Intent intent = new Intent(this, PatientFormsActivity.class);
        intent.putExtra(PATIENT, patient);
        startActivity(intent);
    }

    public void showNotifications(View v) {
        Intent intent = new Intent(this, PatientNotificationActivity.class);
        intent.putExtra(PATIENT, patient);
        startActivity(intent);
    }

    public void showObservations(View v) {
        Intent intent = new Intent(this, ObservationsActivity.class);
        intent.putExtra(PATIENT, patient);
        startActivity(intent);
    }

    public void showEncounters(View v) {
        Intent intent = new Intent(this, EncountersActivity.class);
        intent.putExtra(PATIENT, patient);
        startActivity(intent);
    }

    public void showSHRObservations(View v) {
        Intent intent = new Intent(PatientSummaryActivity.this, SHRObservationsDataActivity.class);
        intent.putExtra(PATIENT, patient);
        startActivity(intent);
    }

    public void switchSyncStatus(View view) {
        imageView.setImageResource(R.drawable.ic_action_shr_synced);
    }

    private static class PatientSummaryActivityMetadata {
        int recommendedForms;
        int incompleteForms;
        int completeForms;
        int newNotifications;
        int totalNotifications;
        int observations;
        int encounters;
    }

    public class BackgroundQueryTask extends AsyncTask<Void, Void, PatientSummaryActivityMetadata> {

        @Override
        protected PatientSummaryActivityMetadata doInBackground(Void... voids) {
            MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
            PatientSummaryActivityMetadata patientSummaryActivityMetadata = new PatientSummaryActivityMetadata();
            FormController formController = muzimaApplication.getFormController();
            NotificationController notificationController = muzimaApplication.getNotificationController();
            ObservationController observationController = muzimaApplication.getObservationController();
            EncounterController encounterController = muzimaApplication.getEncounterController();

            try {
                patientSummaryActivityMetadata.recommendedForms = formController.getRecommendedFormsCount();
                patientSummaryActivityMetadata.completeForms = formController.getCompleteFormsCountForPatient(patient.getUuid());
                patientSummaryActivityMetadata.incompleteForms = formController.getIncompleteFormsCountForPatient(patient.getUuid());
                patientSummaryActivityMetadata.observations = observationController.getObservationsCountByPatient(patient.getUuid());
                patientSummaryActivityMetadata.encounters = encounterController.getEncountersCountByPatient(patient.getUuid());
                User authenticatedUser = ((MuzimaApplication) getApplicationContext()).getAuthenticatedUser();
                if (authenticatedUser != null) {
                    patientSummaryActivityMetadata.newNotifications =
                            notificationController.getNotificationsCountForPatient(patient.getUuid(), authenticatedUser.getPerson().getUuid(),
                                    Constants.NotificationStatusConstants.NOTIFICATION_UNREAD);
                    patientSummaryActivityMetadata.totalNotifications =
                            notificationController.getNotificationsCountForPatient(patient.getUuid(), authenticatedUser.getPerson().getUuid(), null);
                } else {
                    patientSummaryActivityMetadata.newNotifications = 0;
                    patientSummaryActivityMetadata.totalNotifications = 0;
                }
            } catch (FormController.FormFetchException e) {
                Log.w(TAG, "FormFetchException occurred while fetching metadata in MainActivityBackgroundTask", e);
            } catch (NotificationController.NotificationFetchException e) {
                Log.w(TAG, "NotificationFetchException occurred while fetching metadata in MainActivityBackgroundTask", e);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return patientSummaryActivityMetadata;
        }

        @Override
        protected void onPostExecute(PatientSummaryActivityMetadata patientSummaryActivityMetadata) {
            TextView formsDescription = (TextView) findViewById(R.id.formDescription);
            formsDescription.setText(getString(R.string.hint_client_summary_forms, patientSummaryActivityMetadata.incompleteForms,
                    patientSummaryActivityMetadata.completeForms,
                    patientSummaryActivityMetadata.recommendedForms));

            TextView notificationsDescription = (TextView) findViewById(R.id.notificationDescription);
            notificationsDescription.setText(getString(R.string.hint_client_summary_notifications, patientSummaryActivityMetadata.newNotifications,
                    patientSummaryActivityMetadata.totalNotifications));

            TextView observationDescription = (TextView) findViewById(R.id.observationDescription);
            observationDescription.setText(getString(R.string.hint_client_summary_observations, patientSummaryActivityMetadata.observations));

            TextView encounterDescription = (TextView) findViewById(R.id.encounterDescription);
            encounterDescription.setText(getString(R.string.hint_client_summary_encounters, patientSummaryActivityMetadata.encounters));
        }
    }

    private void executeBackgroundTask() {
        mBackgroundQueryTask = new BackgroundQueryTask();
        mBackgroundQueryTask.execute();
    }

    public void prepareWriteToCardOptionDialog(Context context) {

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = layoutInflater.inflate(R.layout.write_to_card_option_dialog_layout, null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PatientSummaryActivity.this);

        writeShrDataOptionDialog = alertBuilder
                .setView(dialogView)
                .create();

        writeShrDataOptionDialog.setCancelable(true);
        searchDialogTextView = (TextView) dialogView.findViewById(R.id.patent_dialog_message_textview);
        yesOptionShrSearchButton = (Button) dialogView.findViewById(R.id.yes_shr_search_dialog);
        noOptionShrSearchButton = (Button) dialogView.findViewById(R.id.no_shr_search_dialog);
        searchDialogTextView.setText(R.string.hint_write_shr_to_card);

        yesOptionShrSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateShrWriteToCard();
            }

        });

        noOptionShrSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissWriteShrDataDialogue();
            }
        });
    }

    public void prepareNonShrWriteToCardOptionDialog(Context context) {

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = layoutInflater.inflate(R.layout.write_to_card_option_dialog_layout, null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PatientSummaryActivity.this);

        writeShrDataOptionDialog = alertBuilder
                .setView(dialogView)
                .create();

        writeShrDataOptionDialog.setCancelable(true);
        searchDialogTextView = (TextView) dialogView.findViewById(R.id.patent_dialog_message_textview);
        yesOptionShrSearchButton = (Button) dialogView.findViewById(R.id.yes_shr_search_dialog);
        noOptionShrSearchButton = (Button) dialogView.findViewById(R.id.no_shr_search_dialog);
        searchDialogTextView.setText(getString(R.string.hint_create_new_shr));

        yesOptionShrSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                if(defaultLocation == null){
                    try {
                        defaultLocation = LocationUtils.getDefaultEncounterLocationPreference(muzimaApplication);
                    } catch (Exception e) {
                        Log.e(TAG,"Could not determine default location",e);
                    }
                }

                if(defaultLocation != null) {
                    registerNewShrRecord();
                } else {
                    dismissWriteShrDataDialogue();

                    AlertDialog alertDialog = new AlertDialog.Builder(PatientSummaryActivity.this).create();
                    alertDialog.setTitle(getString(R.string.general_requirement));
                    alertDialog.setMessage(getString(R.string.hint_set_default_encounter_location_for_shr));
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.general_ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            }

        });

        noOptionShrSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchDialogTextView.setText("");
                dismissWriteShrDataDialogue();
            }
        });
    }

    public void dismissWriteShrDataDialogue(){
        writeShrDataOptionDialog.cancel();
        writeShrDataOptionDialog.dismiss();
    }

    public void readSmartCard(){
        SmartCardIntentIntegrator shrIntegrator = new SmartCardIntentIntegrator(this);
        shrIntegrator.initiateCardRead();
        Toast.makeText(getApplicationContext(), R.string.hint_opening_card_reader, Toast.LENGTH_LONG).show();
    }

    public void processSmartCardReadResult(int requestCode, int resultCode, Intent dataIntent) {
        dismissWriteShrDataDialogue();
        SmartCardIntentResult cardReadIntentResult = null;

        try {
            cardReadIntentResult = SmartCardIntentIntegrator.parseActivityResult(requestCode, resultCode, dataIntent);
        } catch (Exception e) {
            Log.e(TAG, "Could not get result", e);
        }

        if (cardReadIntentResult == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.failure_reading_card), Toast.LENGTH_LONG).show();
            return;
        }

        if (cardReadIntentResult.isSuccessResult()) {
            SmartCardRecord newSmartCardRecord = cardReadIntentResult.getSmartCardRecord();
            if (newSmartCardRecord != null) {
                String shrPayload = newSmartCardRecord.getPlainPayload();
                if(!StringUtils.isEmpty(shrPayload)) {
                    try {
                        KenyaEmrSHRModel kenyaEmrShrModel = KenyaEmrSHRMapper.createSHRModelFromJson(shrPayload);
                        if (kenyaEmrShrModel != null) {
                            if(kenyaEmrShrModel.isNewShrModel()){
                                InternalPatientId shrInternalPatientId = kenyaEmrShrModel.getPatientIdentification()
                                        .getInternalPatientIdByIdentifierType(Constants.Shr.KenyaEmr.PersonIdentifierType.CARD_SERIAL_NUMBER.shr_name);
                                if(shrInternalPatientId != null && !StringUtils.isEmpty(shrInternalPatientId.getID())){
                                    //ToDo: check whether card serial number already assigned
                                    registerNewShrRecord(shrInternalPatientId.getID());
                                } else {
                                    Toast.makeText(getApplicationContext(), getString(R.string.hint_card_blank), Toast.LENGTH_LONG).show();
                                    registerNewShrRecord();
                                }

                            } else {
                                AlertDialog alertDialog = new AlertDialog.Builder(PatientSummaryActivity.this).create();
                                alertDialog.setTitle("Error");
                                alertDialog.setMessage(getString(R.string.hint_card_not_empty));
                                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.general_ok),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }
                        }
                        else {
                            Toast.makeText(getApplicationContext(), getString(R.string.failure_obtaining_card_serial_number), Toast.LENGTH_LONG).show();
                        }
                    } catch (KenyaEmrSHRMapper.ShrParseException e) {
                        Log.e(TAG, "EMR Error ", e);
                    }
                }
            }
        } else {
            /**
             * Card read was interrupted and failed
             */
            Snackbar.make(findViewById(R.id.client_summary_view), "Card read failed." + cardReadIntentResult.getErrors(), Snackbar.LENGTH_LONG)
                    .setAction(R.string.general_retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            readSmartCard();
                        }
                    })
                    .show();
        }
    }
    public void registerNewShrRecord(final String cardSerialNumber) {

        try {
            KenyaEmrSHRModel kenyaEmrShrModel = KenyaEmrSHRMapper.createInitialSHRModelForPatient(muzimaApplication, patient, cardSerialNumber);
            String jsonShrModel = KenyaEmrSHRMapper.createJsonFromSHRModel(kenyaEmrShrModel);

            if (jsonShrModel != null) {

                SmartCardRecord smartCardRecord = new SmartCardRecord();
                smartCardRecord.setPlainPayload(jsonShrModel);
                smartCardRecord.setPersonUuid(patient.getUuid());
                smartCardRecord.setUuid(UUID.randomUUID().toString());
                smartCardRecord.setType(Constants.Shr.KenyaEmr.SMART_CARD_RECORD_TYPE);

                smartCardController.saveSmartCardRecord(smartCardRecord);

                Toast.makeText(getApplicationContext(), "SHR has been Recorded.", Toast.LENGTH_LONG).show();

                //create identifier with card serial number
                KenyaEmrSHRMapper.updatePatientDemographicsWithCardSerialNumberAsIdentifier(muzimaApplication,patient,cardSerialNumber);
                //refresh UI
                dismissWriteShrDataDialogue();
                recreate();

                //write SHR to card
                //initiateShrWriteToCard();
            } else {
                Snackbar.make(findViewById(R.id.client_summary_view), "", Snackbar.LENGTH_LONG)
                        .setAction(R.string.general_retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                registerNewShrRecord(cardSerialNumber);
                            }
                        });
            }


        } catch (KenyaEmrSHRMapper.ShrParseException e) {
            writeShrDataOptionDialog.cancel();
            writeShrDataOptionDialog.dismiss();
            Snackbar.make(findViewById(R.id.client_summary_view),"Unexpected Error Occured "+e.getMessage(),Snackbar.LENGTH_LONG)
                    .setAction(R.string.general_retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            registerNewShrRecord(cardSerialNumber);
                        }
                    })
                    .show();
        } catch (SmartCardController.SmartCardRecordSaveException e) {
            e.printStackTrace();
        }
    }
    public void registerNewShrRecord() {

        try {
            KenyaEmrSHRModel kenyaEmrShrModel = KenyaEmrSHRMapper.createInitialSHRModelForPatient(muzimaApplication, patient);
            String jsonShrModel = KenyaEmrSHRMapper.createJsonFromSHRModel(kenyaEmrShrModel);

            if (jsonShrModel != null) {

                SmartCardRecord smartCardRecord = new SmartCardRecord();
                smartCardRecord.setPlainPayload(jsonShrModel);
                smartCardRecord.setPersonUuid(patient.getUuid());
                smartCardRecord.setUuid(UUID.randomUUID().toString());
                smartCardRecord.setType(Constants.Shr.KenyaEmr.SMART_CARD_RECORD_TYPE);

                smartCardController.saveSmartCardRecord(smartCardRecord);

                Toast.makeText(getApplicationContext(), "SHR has been Recorded.", Toast.LENGTH_LONG).show();

                //create identifier with card serial number
                //KenyaEmrShrMapper.updatePatientDemographicsWithCardSerialNumberAsIdentifier(muzimaApplication,patient,cardSerialNumber);
                //refresh UI
                dismissWriteShrDataDialogue();
                recreate();

                //write SHR to card
                // initiateShrWriteToCard();
            } else {
                Snackbar.make(findViewById(R.id.client_summary_view), "", Snackbar.LENGTH_LONG)
                        .setAction(R.string.general_retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                registerNewShrRecord();
                            }
                        });
            }


        } catch (KenyaEmrSHRMapper.ShrParseException e) {
            writeShrDataOptionDialog.cancel();
            writeShrDataOptionDialog.dismiss();
            Snackbar.make(findViewById(R.id.client_summary_view),getString(R.string.general_unexpected_error_occurred)+e.getMessage(),Snackbar.LENGTH_LONG)
                    .setAction(R.string.general_retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            registerNewShrRecord();
                        }
                    })
                    .show();
        } catch (SmartCardController.SmartCardRecordSaveException e) {
            e.printStackTrace();
        }
    }


}
