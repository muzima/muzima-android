package com.muzima.adapters.patients;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.domain.Credentials;

import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_SUCCESS;
import static com.muzima.utils.Constants.*;
import static com.muzima.utils.DateUtils.getFormattedDate;

public class PatientsRemoteSearchAdapter extends ListAdapter<Patient> {
    private static final String TAG = "PatientsAdapter";
    public static final String SEARCH = "search";
    private PatientController patientController;
    private final String searchString;
    protected BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    public PatientsRemoteSearchAdapter(Context context, int textViewResourceId, PatientController patientController,
                                       String searchString) {
        super(context, textViewResourceId);
        this.patientController = patientController;
        this.searchString = searchString;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_patients_list, parent, false);
            holder = new ViewHolder();
            holder.genderImg = (ImageView) convertView.findViewById(R.id.genderImg);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.dateOfBirth = (TextView) convertView.findViewById(R.id.dateOfBirth);
            holder.identifier = (TextView) convertView.findViewById(R.id.identifier);
            convertView.setTag(holder);
        }

        holder = (ViewHolder) convertView.getTag();

        Patient patient = getItem(position);

        holder.dateOfBirth.setText("DOB: " + getFormattedDate(patient.getBirthdate()));
        holder.identifier.setText(patient.getIdentifier());
        holder.name.setText(getPatientFullName(patient));
        holder.genderImg.setImageResource(getGenderImage(patient.getGender()));
        return convertView;
    }

    private String getPatientFullName(Patient patient) {
        return patient.getFamilyName() + ", " + patient.getGivenName() + " " + patient.getMiddleName();
    }

    private int getGenderImage(String gender) {
        return gender.equalsIgnoreCase("M") ? R.drawable.ic_male : R.drawable.ic_female;
    }

    public BackgroundListQueryTaskListener getBackgroundListQueryTaskListener() {
        return backgroundListQueryTaskListener;
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    @Override
    public void reloadData() {
        new ServerSearchBackgroundTask().execute(searchString);
    }

    private class ServerSearchBackgroundTask extends AsyncTask<String, Void, List<Patient>> {
        long startingTime;

        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }

            startingTime = System.currentTimeMillis();
        }

        @Override
        protected void onPostExecute(List<Patient> patients) {
            if (patients == null) {
                Toast.makeText(getContext(), "Something went wrong while fetching patients from local repo", Toast.LENGTH_SHORT).show();
                return;
            }

            PatientsRemoteSearchAdapter.this.clear();

            for (Patient patient : patients) {
                add(patient);
            }
            notifyDataSetChanged();

            long currentTime = System.currentTimeMillis();
            Log.d(TAG, "Time taken in fetching patients from local repo: " + (currentTime - startingTime) / 1000 + " sec");

            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskFinish();
            }
        }

        @Override
        protected List<Patient> doInBackground(String... strings) {
            MuzimaApplication applicationContext = (MuzimaApplication) getContext();
            addSearchModeToSharedPref(SERVER_SEARCH_MODE);

            Credentials credentials = new Credentials(getContext());
            try {
                int authenticateResult = applicationContext.getMuzimaSyncService().authenticate(credentials.getCredentialsArray());
                if (authenticateResult == AUTHENTICATION_SUCCESS) {
                    return patientController.searchPatientOnServer(strings[0]);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error while searching for patient in the server : " + t);
            } finally {
                applicationContext.getMuzimaContext().closeSession();
            }
            Log.e(TAG, "Authentication failure !! Returning empty patient list");
            return new ArrayList<Patient>();
        }
    }


    private void addSearchModeToSharedPref(String searchMode) {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PATIENT_SEARCH_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PATIENT_SEARCH_PREF_KEY, searchMode);
        editor.commit();
    }

    private class ViewHolder {
        ImageView genderImg;
        TextView name;
        TextView dateOfBirth;
        TextView identifier;
    }
}
