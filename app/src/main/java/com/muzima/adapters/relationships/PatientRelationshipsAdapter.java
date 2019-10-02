/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.adapters.relationships;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
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
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Relationship;
import com.muzima.controller.EncounterController;
import com.muzima.controller.RelationshipController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PatientRelationshipsAdapter extends ListAdapter<Relationship> {
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    private final String patientUuid;
    final RelationshipController relationshipController;


    public PatientRelationshipsAdapter(Activity activity, int textViewResourceId, RelationshipController relationshipController, Patient patient) {
        super(activity, textViewResourceId);
        patientUuid = patient.getUuid();
        this.relationshipController = relationshipController;
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
            convertView = layoutInflater.inflate(R.layout.item_relationship, parent, false);
            convertView.setClickable(false);
            convertView.setFocusable(false);
            holder = new ViewHolder();
            holder.relatedPerson = convertView.findViewById(R.id.relatedPerson);
            holder.relationshipType = convertView.findViewById(R.id.relationshipType);
            holder.genderImg = convertView.findViewById(R.id.genderImg);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.relatedPerson.setText(relationship.getPersonA().getDisplayName());
        holder.relationshipType.setText(relationship.getRelationshipType().getAIsToB());
        holder.genderImg.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_male));

        return convertView;
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    class ViewHolder {
        final LayoutInflater inflater;
        final List<LinearLayout> viewHolders;

        ViewHolder() {
            viewHolders = new ArrayList<>();
            inflater = LayoutInflater.from(getContext());
        }

        TextView relatedPerson;
        TextView relationshipType;
        ImageView genderImg;
    }

    private class BackgroundQueryTask extends AsyncTask<String, Void, List<Relationship>> {
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
                Toast.makeText(getContext(),getContext().getString(R.string.error_encounter_load),Toast.LENGTH_SHORT).show();
                return;
            }
            clear();
            addAll(relationships);
            notifyDataSetChanged();
            backgroundListQueryTaskListener.onQueryTaskFinish();
        }
    }
}