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
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.patients.PatientAdapterHelper;
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
import com.muzima.model.shr.kenyaemr.KenyaEmrShrModel;
import com.muzima.service.JSONInputOutputToDisk;
import com.muzima.utils.Constants;
import com.muzima.utils.smartcard.KenyaEmrShrMapper;
import com.muzima.utils.smartcard.SmartCardIntentIntegrator;
import com.muzima.utils.smartcard.SmartCardIntentResult;
import com.muzima.view.BaseActivity;
import com.muzima.view.SHRObservationsDataActivity;
import com.muzima.view.encounters.EncountersActivity;
import com.muzima.view.forms.PatientFormsActivity;
import com.muzima.view.notifications.PatientNotificationActivity;
import com.muzima.view.observations.ObservationsActivity;
import org.apache.lucene.util.fst.BytesRefFSTEnum;

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
    private PatientController patientController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_summary);

        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            patient = (Patient) intentExtras.getSerializable(PATIENT);
            isRegisteredOnShr = intentExtras.getBoolean("isRegisteredOnShr");
        }

        try {
            setupPatientMetadata();
            notifyOfIdChange();
        } catch (PatientController.PatientLoadException e) {
            Toast.makeText(this, R.string.error_patient_fetch, Toast.LENGTH_SHORT).show();
            finish();
        }
        muzimaApplication = (MuzimaApplication) getApplicationContext();
        patientController = muzimaApplication.getPatientController();

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
                    .setTitle("Notice")
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

    public void invokeShrApplication() {
        SmartCardController smartCardController = ((MuzimaApplication) getApplicationContext()).getSmartCardController();
        SmartCardRecord smartCardRecord = null;
        try {
            KenyaEmrShrMapper.updateSHRSmartCardRecordForPatient((MuzimaApplication) getApplicationContext(),patient.getUuid());
            smartCardRecord = smartCardController.getSmartCardRecordByPersonUuid(patient.getUuid());
        } catch (SmartCardController.SmartCardRecordFetchException e) {
            Snackbar.make(findViewById(R.id.shr_client_summary_view), "Could not fetch smartcard record. "+e.getMessage(), Snackbar.LENGTH_LONG)
                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_dark))
                    .setAction("RETRY", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            invokeShrApplication();
                        }
                    })
                    .show();
            Log.e(TAG, "Could not obtain smartcard record for writing to card", e);
        } catch (KenyaEmrShrMapper.ShrParseException e) {
            Snackbar.make(findViewById(R.id.shr_client_summary_view), "Could not obtain smartcard record for writing to card. "+e.getMessage(), Snackbar.LENGTH_LONG)
                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_dark))
                    .setAction("RETRY", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            invokeShrApplication();
                        }
                    })
                    .show();
            Log.e(TAG, "Could not update smartcard record before writing to card", e);
        }
        if (smartCardRecord != null) {
            SmartCardIntentIntegrator shrIntegrator = new SmartCardIntentIntegrator(this);
            try {
                shrIntegrator.initiateCardWrite(smartCardRecord.getPlainPayload());
            } catch (IOException e) {
                Log.e(TAG, "Could not write to card", e);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(true)
                        .setMessage("Could not write to card. " + e.getMessage())
                        .show();
            }
            Toast.makeText(getApplicationContext(), "Opening Card Reader", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        switch (requestCode) {
            case SMARTCARD_READ_REQUEST_CODE:
                SmartCardIntentResult cardReadIntentResult = null;
                try {
                    cardReadIntentResult = SmartCardIntentIntegrator.parseActivityResult(requestCode, resultCode, dataIntent);
                    if (cardReadIntentResult.isSuccessResult()) {
                        Toast.makeText(this, "Successfully written to card. ", Toast.LENGTH_LONG).show();
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        List<String> errors = cardReadIntentResult.getErrors();
                        if (errors != null && errors.size() > 0) {
                            for (String error : errors) {
                                stringBuilder.append(error);
                            }
                        }

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setCancelable(true)
                                .setMessage("Could not write to card. " + errors)
                                .show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Could not get result", e);
                }
                break;

            case SMARTCARD_WRITE_REQUEST_CODE:
                SmartCardIntentResult cardWriteIntentResult = null;
                try {
                    cardWriteIntentResult = SmartCardIntentIntegrator.parseActivityResult(requestCode, resultCode, dataIntent);
                    List<String> writeErrors = cardWriteIntentResult.getErrors();

                    if (writeErrors == null) {
                        Snackbar.make(findViewById(R.id.shr_client_summary_view), "Smart card data write was successful.", Snackbar.LENGTH_LONG)
                                .show();

                    } else if (writeErrors != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Snackbar.make(findViewById(R.id.shr_client_summary_view), "Smart card data write failed." + writeErrors.get(0), Snackbar.LENGTH_LONG)
                                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_dark, null))
                                    .setAction("RETRY", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            invokeShrApplication();
                                        }
                                    })
                                    .show();
                        } else {

                            Snackbar.make(findViewById(R.id.shr_client_summary_view), "Smart card data write failed." + writeErrors.get(0), Snackbar.LENGTH_LONG)
                                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_dark))
                                    .setAction("RETRY", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            invokeShrApplication();
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
        searchDialogTextView.setText("Do you want to write shr to card ?");

        yesOptionShrSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invokeShrApplication();
            }

        });

        noOptionShrSearchButton.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                writeShrDataOptionDialog.cancel();
                writeShrDataOptionDialog.dismiss();
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
        searchDialogTextView.setText("Client does not have an shr. Do you want to create new shr and write to card ?");

        yesOptionShrSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerNewShrRecord();
            }

        });

        noOptionShrSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchDialogTextView.setText("");
                writeShrDataOptionDialog.cancel();
                writeShrDataOptionDialog.dismiss();
            }
        });
    }


    public void registerNewShrRecord() {

        try {
            KenyaEmrShrModel kenyaEmrShrModel = KenyaEmrShrMapper.createInitialSHRModelForPatient(muzimaApplication, patient);
            String jsonShrModel = KenyaEmrShrMapper.createJsonFromSHRModel(kenyaEmrShrModel);

            if (jsonShrModel != null) {

                SmartCardRecord smartCardRecord = new SmartCardRecord();
                smartCardRecord.setPlainPayload(jsonShrModel);
                smartCardRecord.setPersonUuid(patient.getUuid());
                smartCardRecord.setUuid(UUID.randomUUID().toString());
                smartCardRecord.setType(Constants.Shr.KenyaEmr.SMART_CARD_RECORD_TYPE);

                smartCardController.saveSmartCardRecord(smartCardRecord);

                Toast.makeText(getApplicationContext(), "SHR has been Recorded.", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(PatientSummaryActivity.this, PatientSummaryActivity.class);
                /**
                 * todo check if this patient is registered in shr
                 * before opening PatientSummary activity
                 */
                intent.putExtra("isRegisteredOnShr", true);
                intent.putExtra(PatientSummaryActivity.PATIENT, patient);
                startActivity(intent);

            } else {
                Snackbar.make(findViewById(R.id.shr_client_summary_view), "", Snackbar.LENGTH_LONG)
                        .setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                registerNewShrRecord();
                            }
                        });
            }


        } catch (KenyaEmrShrMapper.ShrParseException e) {
            Log.e(TAG, "Error", e);
        } catch (SmartCardController.SmartCardRecordSaveException e) {
            e.printStackTrace();
        }
    }
}
