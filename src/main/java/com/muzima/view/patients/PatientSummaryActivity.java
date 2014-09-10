/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.patients;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAttribute;
import com.muzima.api.model.User;
import com.muzima.controller.FormController;
import com.muzima.controller.NotificationController;
import com.muzima.controller.PatientController;
import com.muzima.service.JSONInputOutputToDisk;
import com.muzima.utils.Constants;
import com.muzima.view.BaseActivity;
import com.muzima.view.forms.PatientFormsActivity;
import com.muzima.view.notifications.PatientNotificationActivity;

import java.io.IOException;
import java.util.List;

import static com.muzima.utils.DateUtils.getFormattedDate;

public class PatientSummaryActivity extends BaseActivity {
    private static final String TAG = "PatientSummaryActivity";
    public static final String PATIENT = "patient";

    private BackgroundQueryTask mBackgroundQueryTask;

    private Patient patient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_summary);
        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            patient = (Patient) intentExtras.getSerializable(PATIENT);
        }
        try {
            setupPatientMetadata();
            notifyOfIdChange();
            if (!checkFingerprint()) {
                Toast.makeText(this, "Fingerprint has not been captured ", Toast.LENGTH_SHORT).show();
            }
        } catch (PatientController.PatientLoadException e) {
            Toast.makeText(this, "An error occurred while fetching patient", Toast.LENGTH_SHORT).show();
            finish();
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
                    .setMessage("Client Identifier changed on server. The new identifier will be used going forward.")
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
    protected void onStart() {
        super.onStart();
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
        patientName.setText(patient.getFamilyName() + ", " + patient.getGivenName() + " " + patient.getMiddleName());
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
        getSupportMenuInflater().inflate(R.menu.client_summary, menu);
        super.onCreateOptionsMenu(menu);
        return true;
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

    private static class PatientSummaryActivityMetadata {
        int recommendedForms;
        int incompleteForms;
        int completeForms;
        int notifications;
    }

    public class BackgroundQueryTask extends AsyncTask<Void, Void, PatientSummaryActivityMetadata> {

        @Override
        protected PatientSummaryActivityMetadata doInBackground(Void... voids) {
            MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
            PatientSummaryActivityMetadata patientSummaryActivityMetadata = new PatientSummaryActivityMetadata();
            FormController formController = muzimaApplication.getFormController();
            NotificationController notificationController = muzimaApplication.getNotificationController();
            try {
                patientSummaryActivityMetadata.recommendedForms = formController.getRecommendedFormsCount();
                patientSummaryActivityMetadata.completeForms = formController.getCompleteFormsCountForPatient(patient.getUuid());
                patientSummaryActivityMetadata.incompleteForms = formController.getIncompleteFormsCountForPatient(patient.getUuid());
                User authenticatedUser = ((MuzimaApplication) getApplicationContext()).getAuthenticatedUser();
                if (authenticatedUser != null)
                    patientSummaryActivityMetadata.notifications =
                            notificationController.getNotificationsCountForPatient(patient.getUuid(), authenticatedUser.getPerson().getUuid(),
                                    Constants.NotificationStatusConstants.NOTIFICATION_UNREAD);
                else
                    patientSummaryActivityMetadata.notifications = 0;
            } catch (FormController.FormFetchException e) {
                Log.w(TAG, "FormFetchException occurred while fetching metadata in MainActivityBackgroundTask", e);
            } catch (NotificationController.NotificationFetchException e) {
                Log.w(TAG, "NotificationFetchException occurred while fetching metadata in MainActivityBackgroundTask", e);
            }
            return patientSummaryActivityMetadata;
        }

        @Override
        protected void onPostExecute(PatientSummaryActivityMetadata patientSummaryActivityMetadata) {
            TextView formsDescription = (TextView) findViewById(R.id.formDescription);
            formsDescription.setText(patientSummaryActivityMetadata.incompleteForms + " Incomplete, "
                    + patientSummaryActivityMetadata.completeForms + " Complete, "
                    + patientSummaryActivityMetadata.recommendedForms + " Recommended");
            TextView notificationsDescription = (TextView) findViewById(R.id.notificationDescription);
            notificationsDescription.setText(patientSummaryActivityMetadata.notifications + " New Notifications");
        }
    }

    private void executeBackgroundTask() {
        mBackgroundQueryTask = new BackgroundQueryTask();
        mBackgroundQueryTask.execute();
    }

    private boolean checkFingerprint() {
        boolean hasFingerprint = false;
        for (PersonAttribute personAttribute1 : patient.getAtributes()) {
            if (personAttribute1.getAttributeType().getName().equalsIgnoreCase("fingerprint")) {
                hasFingerprint = true;
            }
        }
        return hasFingerprint;
    }
}
