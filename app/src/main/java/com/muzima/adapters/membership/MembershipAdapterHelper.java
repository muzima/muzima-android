/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.membership;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.CohortMembership;
import com.muzima.api.model.Patient;
import com.muzima.utils.Constants.SERVER_CONNECTIVITY_STATUS;
import com.muzima.utils.StringUtils;

import java.util.List;

import static com.muzima.utils.DateUtils.getFormattedDate;

public class MembershipAdapterHelper extends ListAdapter<CohortMembership> {

    public MembershipAdapterHelper(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public View createPatientRow(CohortMembership membership,
                                 View convertView,
                                 ViewGroup parent,
                                 Context context) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.item_memberships_list_multi_checkable, parent, false);
            holder = new ViewHolder();
            holder.genderImg = (ImageView) convertView.findViewById(R.id.genderImg);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.dateOfBirth = (TextView) convertView.findViewById(R.id.dateOfBirth);
            holder.membership = (TextView) convertView.findViewById(R.id.membership);
            holder.identifier = (TextView) convertView.findViewById(R.id.identifier);
            convertView.setTag(holder);
        }

        holder = (ViewHolder) convertView.getTag();

        holder.dateOfBirth.setText("DOB: " + getFormattedDate(membership.getPatient().getBirthdate()));
        holder.membership.setText(getMembershipStatus(holder.membership, membership.isActive()));
        holder.identifier.setText(membership.getPatient().getIdentifier());
        holder.name.setText(getPatientFullName(membership.getPatient()));
        holder.genderImg.setImageResource(getGenderImage(membership.getPatient().getGender()));
        return convertView;
    }

    public void onPreExecute(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        if (backgroundListQueryTaskListener != null) {
            backgroundListQueryTaskListener.onQueryTaskStarted();
        }
    }

    public void onPostExecute(List<CohortMembership> memberships, ListAdapter searchAdapter, BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        if (memberships == null) {
            Toast.makeText(getContext(), getContext()
                    .getString(R.string.error_cohort_membership_repo_fetch),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        searchAdapter.clear();
        searchAdapter.addAll(memberships);
        searchAdapter.notifyDataSetChanged();

        if (backgroundListQueryTaskListener != null) {
            backgroundListQueryTaskListener.onQueryTaskFinish();
        }
    }

    public void onProgressUpdate(List<CohortMembership> memberships, ListAdapter searchAdapter, BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        if (memberships == null) {
            return;
        }

        searchAdapter.addAll(memberships);
        searchAdapter.notifyDataSetChanged();

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
        StringBuffer patientFormattedName = new StringBuffer();
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

    private String getMembershipStatus(TextView textView, boolean status) {
        if (status) {
            textView.setTextColor(getContext().getResources().getColor(R.color.primary_green));
            return getContext().getString(R.string.membership_status_active);
        } else {
            textView.setTextColor(getContext().getResources().getColor(R.color.primary_red));
            return getContext().getString(R.string.membership_status_inactive);
        }
    }

    private String getPatientFullName(Patient patient) {
        StringBuffer patientFullName = new StringBuffer();
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
        return gender.equalsIgnoreCase("M") ? R.drawable.ic_male : R.drawable.ic_female;
    }

    @Override
    public void reloadData() {
    }

    private class ViewHolder {
        ImageView genderImg;
        TextView name;
        TextView dateOfBirth;
        TextView membership;
        TextView identifier;
    }
}