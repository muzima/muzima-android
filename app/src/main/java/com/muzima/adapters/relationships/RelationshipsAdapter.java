/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.adapters.relationships;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.api.model.Relationship;
import com.muzima.controller.PatientController;
import com.muzima.controller.RelationshipController;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RelationshipsAdapter extends ListAdapter<Relationship> {
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    private final String patientUuid;
    private final RelationshipController relationshipController;
    private final PatientController patientController;


    public RelationshipsAdapter(Activity activity, int textViewResourceId, RelationshipController relationshipController,
                                String patientUuid, PatientController patientController) {
        super(activity, textViewResourceId);
        this.patientUuid = patientUuid;
        this.relationshipController = relationshipController;
        this.patientController = patientController;
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute(patientUuid);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Relationship relationship=getItem(position);
        Context context = getContext();
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.item_relationships_list_multi_checkable, parent, false);
            convertView.setClickable(false);
            convertView.setFocusable(false);
            holder = new ViewHolder();
            holder.relatedPerson = convertView.findViewById(R.id.name);
            holder.relationshipType = convertView.findViewById(R.id.relationshipType);
            holder.identifier = convertView.findViewById(R.id.identifier);
            holder.genderImg = convertView.findViewById(R.id.genderImg);
            holder.dateOfBirth = convertView.findViewById(R.id.dateOfBirth);
            holder.age = convertView.findViewById(R.id.age_text_label);
            holder.identifier = convertView.findViewById(R.id.identifier);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (StringUtils.equalsIgnoreCase(patientUuid, relationship.getPersonA().getUuid())) {
            holder.relatedPerson.setText(relationship.getPersonB().getDisplayName());
            holder.relationshipType.setText(relationship.getRelationshipType().getBIsToA());

            Date dob = relationship.getPersonB().getBirthdate();
            if(dob != null) {
                holder.dateOfBirth.setText(String.format("DOB: %s", new SimpleDateFormat("MM-dd-yyyy",
                        Locale.getDefault()).format(dob)));
                holder.age.setText(String.format(Locale.getDefault(), "%d yrs", DateUtils.calculateAge(dob)));
            }else{
                holder.dateOfBirth.setText(String.format(""));
                holder.age.setText(String.format(""));
            }

            if(relationship.getPersonB().getGender() != null) {
                int genderDrawable = relationship.getPersonB().getGender().equalsIgnoreCase("M") ? R.drawable.gender_male : R.drawable.ic_female;
                holder.genderImg.setImageDrawable(getContext().getResources().getDrawable(genderDrawable));
            }
            try {
                Patient p = patientController.getPatientByUuid(relationship.getPersonB().getUuid());
                if (p != null){
                    holder.identifier.setVisibility(View.VISIBLE);
                    holder.identifier.setText(p.getIdentifier());
                } else
                    holder.identifier.setVisibility(View.GONE);
            } catch (PatientController.PatientLoadException e) {
                Log.e(this.getClass().getSimpleName(), "Error searching Patient");
            }
        } else {
            holder.relatedPerson.setText(relationship.getPersonA().getDisplayName());
            holder.relationshipType.setText(relationship.getRelationshipType().getAIsToB());

            Date dob = relationship.getPersonA().getBirthdate();
            if(dob != null) {
                holder.dateOfBirth.setText(String.format("DOB: %s", new SimpleDateFormat("MM-dd-yyyy",
                        Locale.getDefault()).format(dob)));
                holder.age.setText(String.format(Locale.getDefault(), "%d yrs", DateUtils.calculateAge(dob)));
            }else{
                holder.dateOfBirth.setText(String.format(""));
                holder.age.setText(String.format(""));
            }

            if(relationship.getPersonA().getGender() != null) {
                int genderDrawable = relationship.getPersonA().getGender().equalsIgnoreCase("M") ? R.drawable.gender_male : R.drawable.ic_female;
                holder.genderImg.setImageDrawable(getContext().getResources().getDrawable(genderDrawable));
            }
            try {
                Patient p = patientController.getPatientByUuid(relationship.getPersonA().getUuid());
                if (p != null){
                    holder.identifier.setVisibility(View.VISIBLE);
                    holder.identifier.setText(p.getIdentifier());
                } else
                    holder.identifier.setVisibility(View.GONE);
            } catch (PatientController.PatientLoadException e) {
                Log.e(this.getClass().getSimpleName(), "Error searching Patient");
            }
        }

        return convertView;
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public void removeRelationshipsForPatient(String patientUuid, List<Relationship> relationshipsToDelete) {
        try {
            List<Relationship> allRelationshipsForPatient = relationshipController.getRelationshipsForPerson(patientUuid);
            allRelationshipsForPatient.removeAll(relationshipsToDelete);
            try {
                relationshipController.deleteRelationships(relationshipsToDelete);

                //foreach relationship if related person is not synced and has no more relationship then, delete the person
                for (Relationship relationship : relationshipsToDelete) {
                    Person relatedPerson;
                    if (StringUtils.equals(relationship.getPersonA().getUuid(), patientUuid)) {
                        relatedPerson = relationship.getPersonB();
                    } else {
                        relatedPerson = relationship.getPersonA();
                    }

                    if (!relationship.getSynced() &&
                            relationshipController.getRelationshipsForPerson(relatedPerson.getUuid()).size() < 1 ){
                        try {
                            relationshipController.deletePerson(relatedPerson);
                        } catch (RelationshipController.DeletePersonException e) {
                            Log.e(getClass().getSimpleName(), "Error while deleting last person", e);
                        }
                    }
                }
            } catch (RelationshipController.DeleteRelationshipException e) {
                Log.e(getClass().getSimpleName(), "Error while deleting the relationships", e);
            }
            clear();
            addAll(allRelationshipsForPatient);
        } catch (RelationshipController.RetrieveRelationshipException e) {
            Log.e(getClass().getSimpleName(), "Error while fetching the relationships", e);
        }
    }

    class ViewHolder {
        ImageView genderImg;
        TextView dateOfBirth;
        TextView age;
        TextView identifier;

        TextView relatedPerson;
        TextView relationshipType;
    }

    private class BackgroundQueryTask extends MuzimaAsyncTask<String, Void, List<Relationship>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected List<Relationship> doInBackground(String... params) {
            List<Relationship> relationships = null;
            try {
               relationships = relationshipController.getRelationshipsForPerson(patientUuid);

            }catch(RelationshipController.RetrieveRelationshipException e){
                Log.e(this.getClass().getSimpleName(),"Could not get relationship for patient",e);
            }

            return relationships;
        }

        @Override
        protected void onPostExecute(List<Relationship> relationships){
            if(relationships==null){
                Toast.makeText(getContext(),getContext().getString(R.string.error_relationship_load),Toast.LENGTH_SHORT).show();
                return;
            }
            clear();
            addAll(relationships);
            notifyDataSetChanged();
            backgroundListQueryTaskListener.onQueryTaskFinish();
        }

        @Override
        protected void onBackgroundError(Exception e) {

        }
    }
}
