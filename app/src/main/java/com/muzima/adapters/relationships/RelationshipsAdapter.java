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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.muzima.MuzimaApplication;
import com.muzima.R;

import com.muzima.adapters.RecyclerAdapter;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonTag;
import com.muzima.api.model.Relationship;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.PersonController;
import com.muzima.controller.RelationshipController;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;

import com.muzima.utils.ConceptUtils;

public class RelationshipsAdapter extends RecyclerAdapter<Relationship> {
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    private final String patientUuid;
    private final RelationshipController relationshipController;
    private final PatientController patientController;
    private MuzimaApplication muzimaApplication;
    private ConceptController conceptController;
    private ObservationController observationController;
    private Context context;
    private PersonController personController;
    private List<Relationship> relationshipList;
    private RelationshipListClickListener relationshipListClickListener;
    private List<Relationship> selectedRelationships;


    public RelationshipsAdapter(Activity activity, int textViewResourceId, RelationshipController relationshipController,
                                String patientUuid, PatientController patientController) {
        this.patientUuid = patientUuid;
        this.relationshipController = relationshipController;
        this.patientController = patientController;
        muzimaApplication = (MuzimaApplication) activity.getApplicationContext();
        conceptController = muzimaApplication.getConceptController();
        observationController = muzimaApplication.getObservationController();
        personController = muzimaApplication.getPersonController();
        relationshipList = new ArrayList<>();
        selectedRelationships = new ArrayList<>();
        context = activity;
    }

    @NonNull
    @Override
    public RelationshipsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.item_relationships_list_multi_checkable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.ViewHolder holder, int position) {
        bindViews((RelationshipsAdapter.ViewHolder) holder, position);
        Relationship relationship=relationshipList.get(position);

        ((ViewHolder) holder).lessMore.setOnClickListener(v -> {
            boolean expanded = relationship.isExpanded();
            relationship.setExpanded(!expanded);
            notifyItemChanged(position);
        });
    }

    private void bindViews(@NonNull RelationshipsAdapter.ViewHolder holder, int position) {
        Relationship relationship=relationshipList.get(position);

        String relatedPersonUuid = "";

        boolean expanded = relationship.isExpanded();
        holder.hivTestDetails.setVisibility(expanded ? View.VISIBLE : View.GONE);
        holder.hivCareDetails.setVisibility(expanded ? View.VISIBLE : View.GONE);
        holder.lessMore.setText(expanded ? R.string.general_less : R.string.general_more);

        if (StringUtils.equalsIgnoreCase(patientUuid, relationship.getPersonA().getUuid())) {
            relatedPersonUuid = relationship.getPersonB().getUuid();
            holder.relatedPerson.setText(relationship.getPersonB().getDisplayName());
            holder.relationshipType.setText(relationship.getRelationshipType().getBIsToA());

            Date dob = relationship.getPersonB().getBirthdate();
            if(dob != null) {
                holder.dateOfBirth.setText(context.getString(R.string.general_date_of_birth ,String.format(" %s", new SimpleDateFormat("dd-MM-yyyy",
                        Locale.getDefault()).format(dob))));

                holder.age.setText(context.getString(R.string.general_years ,String.format(Locale.getDefault(), "%d ", DateUtils.calculateAge(dob))));
            }else{
                holder.dateOfBirth.setText(String.format(""));
                holder.age.setText(String.format(""));
            }

            if(relationship.getPersonB().getGender() != null) {
                int genderDrawable = relationship.getPersonB().getGender().equalsIgnoreCase("M") ? R.drawable.gender_male : R.drawable.gender_female;
                holder.genderImg.setImageDrawable(context.getResources().getDrawable(genderDrawable));
            }
            else{
               holder.genderImg.setImageDrawable(context.getResources().getDrawable(R.drawable.generic_person));
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
            relatedPersonUuid = relationship.getPersonA().getUuid();
            holder.relatedPerson.setText(relationship.getPersonA().getDisplayName());
            holder.relationshipType.setText(relationship.getRelationshipType().getAIsToB());

            Date dob = relationship.getPersonA().getBirthdate();
            if(dob != null) {
                holder.dateOfBirth.setText(context.getString(R.string.general_date_of_birth ,String.format(" %s", new SimpleDateFormat("dd-MM-yyyy",
                        Locale.getDefault()).format(dob))));
                holder.age.setText(context.getString(R.string.general_years ,String.format(Locale.getDefault(), "%d ", DateUtils.calculateAge(dob))));
            }else{
                holder.dateOfBirth.setText(String.format(""));
                holder.age.setText(String.format(""));
            }

            if(relationship.getPersonA().getGender() != null && !StringUtils.isEmpty(relationship.getPersonA().getGender())) {
                int genderDrawable = relationship.getPersonA().getGender().equalsIgnoreCase("M") ? R.drawable.gender_male : R.drawable.ic_female;
                holder.genderImg.setImageDrawable(context.getResources().getDrawable(genderDrawable));
            }
            else{
                holder.genderImg.setImageDrawable(context.getResources().getDrawable(R.drawable.generic_person));
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

        if(!muzimaApplication.getMuzimaSettingController().isFGHCustomClientSummaryEnabled()){
            holder.hivTestDetails.setVisibility(View.GONE);
            holder.hivCareDetails.setVisibility(View.GONE);
        }else {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String applicationLanguage = preferences.getString(context.getResources().getString(R.string.preference_app_language), context.getResources().getString(R.string.language_portuguese));

            try {
                holder.testDate.setText(getObsDateTimeByPatientUuidAndConceptId(relatedPersonUuid, 23779, observationController, conceptController, applicationLanguage));
                holder.results.setText(getObsByPatientUuidAndConceptId(relatedPersonUuid, 23779, observationController, conceptController, applicationLanguage));
                holder.inHivCare.setText(getObsByPatientUuidAndConceptId(relatedPersonUuid, 23780, observationController, conceptController, applicationLanguage));
                holder.inCCR.setText(getObsByPatientUuidAndConceptId(relatedPersonUuid, 1885, observationController, conceptController, applicationLanguage));

            } catch (JSONException e) {
                Log.e(getClass().getSimpleName(),"Encountered JSONException ",e);
            } catch (ObservationController.LoadObservationException e) {
                Log.e(getClass().getSimpleName(),"Encountered LoadObservationException ",e);
            }
        }
        try {
            Person person = personController.getPersonByUuid(relatedPersonUuid);
            if(person != null){
                addTags(holder,person);
            }
        } catch (PersonController.PersonLoadException e) {
            Log.e(getClass().getSimpleName(), "Encountered an exception while loading persons");
        }

        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(relationshipListClickListener != null) {
                    relationshipListClickListener.onItemClick(view, position);

                }
            }
        });

        holder.container.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(relationshipListClickListener != null) {
                    relationshipListClickListener.onItemLongClick(view, position);
                }
                return true;
            }
        });
    }

    public void setRelationshipListClickListener(RelationshipListClickListener relationshipListClickListener) {
        this.relationshipListClickListener = relationshipListClickListener;
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute(patientUuid);
    }

    public String getObsByPatientUuidAndConceptId(String patientUuid, int conceptId, ObservationController observationController, ConceptController conceptController, String applicationLanguage) throws JSONException, ObservationController.LoadObservationException {
        List<Observation> observations = new ArrayList<>();
        try {
            observations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, conceptId);
            Concept concept = conceptController.getConceptById(conceptId);
            Collections.sort(observations, observationDateTimeComparator);
            if(observations.size()>0){
                Observation obs = observations.get(0);
                if(concept.isDatetime())
                    return DateUtils.getFormattedDate(obs.getValueDatetime(), DateUtils.SIMPLE_DAY_MONTH_YEAR_DATE_FORMAT);
                else if(concept.isCoded())
                    return ConceptUtils.getConceptNameFromConceptNamesByLocale(obs.getValueCoded().getConceptNames(),applicationLanguage);
                else if(concept.isNumeric())
                    return String.valueOf(obs.getValueNumeric());
                else
                    return obs.getValueText();
            }
        } catch (ObservationController.LoadObservationException | Exception | ConceptController.ConceptFetchException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading observations", e);
        }
        return StringUtils.EMPTY;
    }

    private String getObsDateTimeByPatientUuidAndConceptId(String patientUuid, int conceptId, ObservationController observationController, ConceptController conceptController, String applicationLanguage) throws JSONException, ObservationController.LoadObservationException {
        List<Observation> observations = new ArrayList<>();
        try {
            observations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, conceptId);
            Collections.sort(observations, observationDateTimeComparator);
            if(observations.size()>0){
                Observation obs = observations.get(0);
                return DateUtils.getFormattedDate(obs.getObservationDatetime(), DateUtils.SIMPLE_DAY_MONTH_YEAR_DATE_FORMAT);
            }
        } catch (ObservationController.LoadObservationException | Exception  e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading observations", e);
        }
        return StringUtils.EMPTY;
    }

    private final Comparator<Observation> observationDateTimeComparator = new Comparator<Observation>() {
        @Override
        public int compare(Observation lhs, Observation rhs) {
            return -lhs.getObservationDatetime().compareTo(rhs.getObservationDatetime());
        }
    };

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
            relationshipList.clear();
            relationshipList.addAll(allRelationshipsForPatient);
        } catch (RelationshipController.RetrieveRelationshipException e) {
            Log.e(getClass().getSimpleName(), "Error while fetching the relationships", e);
        }
    }

    @Override
    public int getItemCount() {
        return relationshipList.size();
    }

    public class ViewHolder extends RecyclerAdapter.ViewHolder{
        ImageView genderImg;
        TextView dateOfBirth;
        TextView age;
        TextView identifier;

        TextView relatedPerson;
        TextView relationshipType;
        TextView testDate;
        TextView results;
        TextView inHivCare;
        TextView inCCR;
        RelativeLayout hivTestDetails;
        RelativeLayout hivCareDetails;

        List<TextView> tags;
        LinearLayout tagsLayout;
        RelativeLayout container;
        TextView lessMore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            relatedPerson = itemView.findViewById(R.id.name);
            relationshipType = itemView.findViewById(R.id.relationshipType);
            identifier = itemView.findViewById(R.id.identifier);
            genderImg = itemView.findViewById(R.id.genderImg);
            dateOfBirth = itemView.findViewById(R.id.dateOfBirth);
            age = itemView.findViewById(R.id.age_text_label);
            identifier = itemView.findViewById(R.id.identifier);
            testDate = itemView.findViewById(R.id.hiv_test_date);
            results = itemView.findViewById(R.id.hiv_results);
            inHivCare = itemView.findViewById(R.id.in_hiv_care);
            inCCR = itemView.findViewById(R.id.in_ccr);
            hivTestDetails = itemView.findViewById(R.id.hiv_test_details);
            hivCareDetails = itemView.findViewById(R.id.hiv_care_details);
            tagsLayout = itemView.findViewById(R.id.menu_tags);
            container = itemView.findViewById(R.id.item_patient_container);
            lessMore = itemView.findViewById(R.id.hiv_details_more_less);
            tags = new ArrayList<>();
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
            tagsLayout.removeAllViews();
        }
    }


    private void addTags(RelationshipsAdapter.ViewHolder holder, Person person) {
        PersonTag[] tags = person.getPersonTags();
        if(tags!=null) {
            if (tags.length > 0) {
                LayoutInflater layoutInflater = LayoutInflater.from(context);

                //add update tags
                for (int i = 0; i < tags.length; i++) {
                    TextView textView = null;
                    if (holder.tags.size() <= i) {
                        textView = newTextView(layoutInflater);
                        holder.addTag(textView);
                    }
                    textView = holder.tags.get(i);
                    textView.setBackgroundColor(personController.getTagColor(tags[i].getUuid()));
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
                holder.tags.clear();
                holder.tagsLayout.removeAllViews();
            }
        }
    }

    private TextView newTextView(LayoutInflater layoutInflater) {
        TextView textView = (TextView) layoutInflater.inflate(R.layout.tag, null, false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(1, 0, 0, 0);
        textView.setLayoutParams(layoutParams);
        return textView;
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
                Toast.makeText(context,context.getString(R.string.error_relationship_load),Toast.LENGTH_SHORT).show();
                return;
            }
            relationshipList.clear();
            relationshipList.addAll(relationships);
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskFinish();
            }
            notifyDataSetChanged();
        }

        @Override
        protected void onBackgroundError(Exception e) {

        }
    }

    public boolean isEmpty(){
        return relationshipList.isEmpty();
    }

    public interface RelationshipListClickListener {
        void onItemLongClick(View view, int position);
        void onItemClick(View view, int position);
    }

    public Relationship getRelationship(int position){
       Relationship relationship =  relationshipList.get(position);
       return relationship;
    }

    public void toggleSelection(View view, int position){
        Relationship relationship = relationshipList.get(position);
        if (!selectedRelationships.contains(relationship)) {
            selectedRelationships.add(relationship);
        } else if (selectedRelationships.contains(relationship)) {
            selectedRelationships.remove(relationship.getUuid());
        }
    }

    public List<Relationship> getSelectedRelationships() {
        return selectedRelationships;
    }

    public void resetSelectedRelationships() {
        selectedRelationships = new ArrayList<>();
    }
}
