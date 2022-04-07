/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.patients;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAddress;
import com.muzima.api.model.PatientTag;
import com.muzima.controller.PatientController;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.model.patient.PatientItem;
import com.muzima.utils.Constants.SERVER_CONNECTIVITY_STATUS;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import com.muzima.view.custom.CheckedLinearLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;

import static com.muzima.utils.DateUtils.getFormattedDate;

public class PatientAdapterHelper extends RecyclerAdapter<PatientAdapterHelper.ViewHolder> {
    private PatientController patientController;
    private MuzimaGPSLocation currentLocation;
    private Context context;
    private List<PatientItem> patientList;
    private List<String> selectedPatientsUuids;
    private PatientListClickListener patientListClickListener;
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    public PatientAdapterHelper(Context context, PatientController patientController,PatientListClickListener patientListClickListener) {
        this.patientController = patientController;
        this.context = context;
        patientList = new ArrayList<>();
        selectedPatientsUuids = new ArrayList<>();
        this.patientListClickListener = patientListClickListener;
    }

    public void setCurrentLocation(MuzimaGPSLocation currentLocation) {
        this.currentLocation = currentLocation;
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    @NonNull
    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view  = layoutInflater.inflate(R.layout.item_patients_list_multi_checkable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.ViewHolder holder, int position) {
        bindViews((ViewHolder) holder,position);
    }

    private void bindViews(PatientAdapterHelper.ViewHolder holder, int position){
        Patient patient = patientList.get(position).getPatient();

        if(patient.getBirthdate() != null) {
            holder.dateOfBirth.setText(String.format("DOB: %s", getFormattedDate(patient.getBirthdate())));
        }else{
            holder.dateOfBirth.setText(String.format(""));
        }
        Date dob = patient.getBirthdate();
        if(dob != null) {
            holder.dateOfBirth.setText(String.format("DOB: %s", new SimpleDateFormat("MM-dd-yyyy",
                    Locale.getDefault()).format(dob)));
            holder.age.setText(String.format(Locale.getDefault(), "%d yrs", DateUtils.calculateAge(dob)));
        }else{
            holder.dateOfBirth.setText(String.format(""));
            holder.age.setText(String.format(""));
        }

        holder.identifier.setText(patient.getIdentifier());
        holder.distanceToClientAddress.setText(getDistanceToClientAddress(patient));
        holder.name.setText(getPatientFullName(patient));
        if(patient.getGender() != null) {
            holder.genderImg.setImageResource(getGenderImage(patient.getGender()));
        }
        addTags(holder,patient);
        highlightPatientItem(patient, holder.container);

        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                patientListClickListener.onItemClick(view,position);
            }
        });

        holder.container.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                toggleSelection(patient, (CheckedLinearLayout) view);
                patientListClickListener.onItemLongClick(view,position);
                return false;
            }
        });
    }

    private void highlightPatientItem(Patient patient, CheckedLinearLayout view){
        if(selectedPatientsUuids.contains(patient.getUuid())){
            //highlight
            view.setActivated(true);
        } else {
            //render as not highlighted
            view.setActivated(false);
        }
    }

    public void toggleSelection(Patient patient, CheckedLinearLayout view){
        CheckedLinearLayout checkedLinearLayout = (CheckedLinearLayout) view;
        checkedLinearLayout.toggle();
        boolean selected = checkedLinearLayout.isChecked();

        if (selected && !selectedPatientsUuids.contains(patient.getUuid())) {
            selectedPatientsUuids.add(patient.getUuid());
            checkedLinearLayout.setActivated(true);
        } else if (!selected && selectedPatientsUuids.contains(patient.getUuid())) {
            selectedPatientsUuids.remove(patient.getUuid());
            checkedLinearLayout.setActivated(false);
        }
    }

    public List<String> getSelectedPatientsUuids() {
        return selectedPatientsUuids;
    }

    public void resetSelectedPatientsUuids() {
        selectedPatientsUuids = new ArrayList<>();
    }

    @Override
    public void reloadData() {
    }

    @Override
    public int getItemCount() {
        return patientList.size();
    }

    public

    private String getDistanceToClientAddress(Patient patient){
        PersonAddress personAddress = patient.getPreferredAddress();
        if (currentLocation != null && personAddress != null && !StringUtils.isEmpty(personAddress.getLatitude()) && !StringUtils.isEmpty(personAddress.getLongitude())) {
            double startLatitude = Double.parseDouble(currentLocation.getLatitude());
            double startLongitude = Double.parseDouble(currentLocation.getLongitude());
            double endLatitude = Double.parseDouble(personAddress.getLatitude());
            double endLongitude= Double.parseDouble(personAddress.getLongitude());

            float[] results = new float[1];
            Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results);
            return String.format("%.02f",results[0]/1000) + " km";
        }
        return "";
    }

    private void addTags(ViewHolder holder, Patient patient) {
        PatientTag[] tags = patient.getTags();
        if (tags.length > 0) {
//            holder.tagsScroller.setVisibility(View.VISIBLE);
            LayoutInflater layoutInflater = LayoutInflater.from(context);

            //add update tags
            for (int i = 0; i < tags.length; i++) {
                TextView textView = null;
                if (holder.tags.size() <= i) {
                    textView = newTextView(layoutInflater);
                    holder.addTag(textView);
                }
                textView = holder.tags.get(i);
                textView.setBackgroundColor(patientController.getTagColor(tags[i].getUuid()));
                textView.setText(tags[i].getName());
            }

            //remove existing extra tags which are present because of recycled list view
            if (tags.length < holder.tags.size()) {
                List<TextView> tagsToRemove = new ArrayList<>();
                for (int i = tags.length; i < holder.tags.size(); i++) {
                    tagsToRemove.add(holder.tags.get(i));
                }
                holder.removeTags(tagsToRemove);
            }
        } else {
//            holder.tagsScroller.setVisibility(View.INVISIBLE);
        }
    }

    private TextView newTextView(LayoutInflater layoutInflater) {
        TextView textView = (TextView) layoutInflater.inflate(R.layout.tag, null, false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(1, 0, 0, 0);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    protected void onPreExecute(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        if (backgroundListQueryTaskListener != null) {
            backgroundListQueryTaskListener.onQueryTaskStarted();
        }
    }

    protected void onPostExecute(List<Patient> patients, BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        if (patients == null) {
            Toast.makeText(context, context.getString(R.string.error_patient_repo_fetch), Toast.LENGTH_SHORT).show();
            return;
        }

        patientList.clear();
        for(Patient patient:patients) {
            patientList.add(new PatientItem(patient));
        }
        notifyDataSetChanged();

        if (backgroundListQueryTaskListener != null) {
            backgroundListQueryTaskListener.onQueryTaskFinish();
        }
    }

    protected void onProgressUpdate(List<Patient> patients) {
        if (patients == null) {
            return;
        }

        for(Patient patient:patients) {
            patientList.add(new PatientItem(patient));
        }
        notifyDataSetChanged();

        if (backgroundListQueryTaskListener != null) {
            backgroundListQueryTaskListener.onQueryTaskFinish();
        }

    }

    public void onAuthenticationError(int searchResutStatus, BackgroundListQueryTaskListener backgroundListQueryTaskListener){
        backgroundListQueryTaskListener.onQueryTaskCancelled(searchResutStatus);
    }

    public void onNetworkError(SERVER_CONNECTIVITY_STATUS networkStatus, BackgroundListQueryTaskListener backgroundListQueryTaskListener){
        if (backgroundListQueryTaskListener != null) {
            backgroundListQueryTaskListener.onQueryTaskCancelled(networkStatus);
        }
    }

    public static String getPatientFormattedName(Patient patient) {
        StringBuilder patientFormattedName = new StringBuilder();
        if (!StringUtils.isEmpty(patient.getFamilyName())) {
            patientFormattedName.append(patient.getFamilyName());
            patientFormattedName.append(", ");
        }
        if (!StringUtils.isEmpty(patient.getGivenName())) {
            patientFormattedName.append(patient.getGivenName().substring(0, 1));
            patientFormattedName.append(" ");
        }
        if (!StringUtils.isEmpty(patient.getMiddleName())) {
            patientFormattedName.append(patient.getMiddleName().substring(0, 1));
        }
        return patientFormattedName.toString();
    }

    private String getPatientFullName(Patient patient) {
        StringBuilder patientFullName = new StringBuilder();
        if (!StringUtils.isEmpty(patient.getFamilyName())) {
            patientFullName.append(patient.getFamilyName());
            patientFullName.append(", ");
        }
        if (!StringUtils.isEmpty(patient.getGivenName())) {
            patientFullName.append(patient.getGivenName());
            patientFullName.append(" ");
        }
        if (!StringUtils.isEmpty(patient.getMiddleName())) {
            patientFullName.append(patient.getMiddleName());
        }
        return patientFullName.toString();
    }

    private int getGenderImage(String gender) {
        return gender.equalsIgnoreCase("M") ? R.drawable.gender_male : R.drawable.gender_female;
    }

    public class ViewHolder extends RecyclerAdapter.ViewHolder{
        ImageView genderImg;
        TextView name;
        TextView dateOfBirth;
        TextView age;
        TextView identifier;
        TextView distanceToClientAddress;
        List<TextView> tags;
        LinearLayout tagsLayout;
        CheckedLinearLayout container;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            genderImg = itemView.findViewById(R.id.genderImg);
            name = itemView.findViewById(R.id.name);
            dateOfBirth = itemView.findViewById(R.id.dateOfBirth);
            age = itemView.findViewById(R.id.age_text_label);
            distanceToClientAddress = itemView.findViewById(R.id.distanceToClientAddress);
            identifier = itemView.findViewById(R.id.identifier);
            tagsLayout = itemView.findViewById(R.id.menu_tags);
            tags = new ArrayList<>();
            container = itemView.findViewById(R.id.item_patient_container);
        }

        public void addTag(TextView tag) {
            this.tags.add(tag);
            tagsLayout.addView(tag);
        }

        void removeTags(List<TextView> tagsToRemove) {
            for (TextView tag : tagsToRemove) {
                tagsLayout.removeView(tag);
            }
            tags.removeAll(tagsToRemove);
        }
    }

    public interface PatientListClickListener {

        void onItemLongClick(View view, int position);
        void onItemClick(View view, int position);
    }
}
