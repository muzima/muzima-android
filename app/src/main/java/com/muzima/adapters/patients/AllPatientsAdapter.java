package com.muzima.adapters.patients;

import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAddress;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AllPatientsAdapter extends RecyclerView.Adapter<AllPatientsAdapter.ViewHolder> {

    private List<Patient> patientList;
    private OnPatientClickedListener patientClickedListener;
    private MuzimaGPSLocation currentLocation;

    public AllPatientsAdapter(List<Patient> patientList, OnPatientClickedListener patientClickedListener, MuzimaGPSLocation currentLocation) {
        this.patientList = patientList;
        this.patientClickedListener = patientClickedListener;
        this.currentLocation = currentLocation;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patients_list_multi_checkable, parent, false);
        return new ViewHolder(view, patientClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Patient patient = patientList.get(position);
        holder.patientNameTextView.setText(patient.getDisplayName());
        holder.identifierTextView.setText(patient.getIdentifier());
        holder.dobTextView.setText(new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
                .format(patient.getBirthdate()));
        holder.distanceToAddressTextView.setText(getDistanceToClientAddress(patient));
    }

    private String getDistanceToClientAddress(Patient patient) {
        try {
            PersonAddress personAddress = patient.getPreferredAddress();
            if (currentLocation != null && personAddress != null && !StringUtils.isEmpty(personAddress.getLatitude()) && !StringUtils.isEmpty(personAddress.getLongitude())) {
                double startLatitude = Double.parseDouble(currentLocation.getLatitude());
                double startLongitude = Double.parseDouble(currentLocation.getLongitude());
                double endLatitude = Double.parseDouble(personAddress.getLatitude());
                double endLongitude = Double.parseDouble(personAddress.getLongitude());

                float[] results = new float[1];
                Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results);
                return String.format("%.02f", results[0] / 1000) + " km";
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public int getItemCount() {
        return patientList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final View container;
        private final ImageView genderImageView;
        private final TextView patientNameTextView;
        private final TextView dobTextView;
        private final TextView identifierTextView;
        private final TextView distanceToAddressTextView;
        private final OnPatientClickedListener patientClickedListener;

        public ViewHolder(@NonNull View itemView, OnPatientClickedListener patientClickedListener) {
            super(itemView);

            container = itemView.findViewById(R.id.item_patient_container);
            genderImageView = itemView.findViewById(R.id.genderImg);
            patientNameTextView = itemView.findViewById(R.id.name);
            dobTextView = itemView.findViewById(R.id.dateOfBirth);
            identifierTextView = itemView.findViewById(R.id.identifier);
            distanceToAddressTextView = itemView.findViewById(R.id.distanceToClientAddress);
            this.patientClickedListener = patientClickedListener;

            container.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            patientClickedListener.onPatientClicked(getAdapterPosition());
        }
    }

    public interface OnPatientClickedListener {
        void onPatientClicked(int position);
    }
}
