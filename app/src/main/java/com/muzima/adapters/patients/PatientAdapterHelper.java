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
import android.content.res.Configuration;
import android.location.Location;
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
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PersonAddress;
import com.muzima.api.model.PatientTag;
import com.muzima.api.model.PersonAttribute;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.PatientController;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.model.patient.PatientItem;
import com.muzima.utils.DateUtils;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.view.custom.CheckedLinearLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;

import static com.muzima.util.Constants.ServerSettings.PATIENT_ADDITIONAL_DETAILS;
import static com.muzima.utils.DateUtils.getFormattedDate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class PatientAdapterHelper extends RecyclerAdapter<PatientAdapterHelper.ViewHolder> {
    private PatientController patientController;
    private MuzimaGPSLocation currentLocation;
    private Context context;
    private List<PatientItem> patientList;
    private List<String> selectedPatientsUuids;
    private PatientListClickListener patientListClickListener;
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    private final LanguageUtil languageUtil = new LanguageUtil();
    private Configuration configuration ;
    private MuzimaSettingController muzimaSettingController;
    private boolean showAdditionalDetails;

    public PatientAdapterHelper(Context context, PatientController patientController, MuzimaSettingController muzimaSettingController) {
        this.patientController = patientController;
        this.muzimaSettingController = muzimaSettingController;
        this.context = context;
        patientList = new ArrayList<>();
        selectedPatientsUuids = new ArrayList<>();

        configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(languageUtil.getSelectedLocale(context));
    }

    protected Context getContext(){
        return context;
    }

    public void setCurrentLocation(MuzimaGPSLocation currentLocation) {
        this.currentLocation = currentLocation;
    }

    public void setShowAdditionalDetails(boolean showAdditionalDetails){
        this.showAdditionalDetails = showAdditionalDetails;
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    protected BackgroundListQueryTaskListener getBackgroundListQueryTaskListener() {
        return backgroundListQueryTaskListener;
    }

    public void setPatientListClickListener(PatientListClickListener patientListClickListener) {
        this.patientListClickListener = patientListClickListener;
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
            holder.dateOfBirth.setText(String.format("DOB: %s", DateUtils.getFormattedDate(patient.getBirthdate())));
        }else{
            holder.dateOfBirth.setText(String.format(""));
        }
        Date dob = patient.getBirthdate();
        if(dob != null) {
            holder.dateOfBirth.setText(context.createConfigurationContext(configuration).getResources().getString(R.string.general_date_of_birth ,String.format(" %s", new SimpleDateFormat("MM-dd-yyyy",
                    Locale.getDefault()).format(dob))));
            holder.age.setText(context.createConfigurationContext(configuration).getResources().getString(R.string.general_years ,String.format(Locale.getDefault(), "%d ", DateUtils.calculateAge(dob))));
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
        if(showAdditionalDetails) {
            addAdditionalDetails(holder, patient);
        }
        highlightPatientItem(patient, holder.container);

        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(patientListClickListener != null) {
                    patientListClickListener.onItemClick(view, position);
                }
            }
        });

        holder.container.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(patientListClickListener != null) {
                    patientListClickListener.onItemLongClick(view, position);
                }
                return true;
            }
        });
    }

    private void highlightPatientItem(Patient patient, CheckedLinearLayout view){
        if(selectedPatientsUuids.contains(patient.getUuid())){
            //highlight
            view.setActivated(true);
            view.setChecked(true);
        } else {
            //render as not highlighted
            view.setActivated(false);
            view.setChecked(false);
        }
    }

    public void toggleSelection(View view, int position){
        CheckedLinearLayout checkedLinearLayout = (CheckedLinearLayout)view;
        checkedLinearLayout.toggle();
        boolean selected = checkedLinearLayout.isChecked();
        Patient patient = patientList.get(position).getPatient();

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
    public abstract  void reloadData();

    @Override
    public int getItemCount() {
        return patientList.size();
    }

    public Patient getPatient(int position){
        PatientItem item = patientList.get(position);

        if(item != null){
            return item.getPatient();
        }
        return null;
    }

    public boolean isEmpty(){
        return patientList.isEmpty();
    }

    private String getDistanceToClientAddress(Patient patient){
        PersonAddress personAddress = patient.getPreferredAddress();
        if (currentLocation != null && personAddress != null && !StringUtils.isEmpty(personAddress.getLatitude()) && !StringUtils.isEmpty(personAddress.getLongitude())) {
            try {
                double startLatitude = Double.parseDouble(currentLocation.getLatitude());
                double startLongitude = Double.parseDouble(currentLocation.getLongitude());
                double endLatitude = Double.parseDouble(personAddress.getLatitude());
                double endLongitude = Double.parseDouble(personAddress.getLongitude());

                float[] results = new float[1];
                Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results);
                return String.format("%.02f", results[0] / 1000) + " km";
            }catch (NumberFormatException e){
                Log.e(getClass().getSimpleName(),"Number format exception encountered while parsing number ",e);
            }
        }
        return "";
    }

    private void addTags(ViewHolder holder, Patient patient) {
        PatientTag[] tags = patient.getTags();
        if(tags!=null) {
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
            }
        }else {
//            holder.tagsScroller.setVisibility(View.INVISIBLE);
        }
    }

    public void addAdditionalDetails(ViewHolder holder, Patient patient){
        try {
            int index = 1;
            MuzimaSetting muzimaSetting = ((MuzimaApplication) context.getApplicationContext()).getMuzimaSettingController().getSettingByProperty(PATIENT_ADDITIONAL_DETAILS);
            JSONObject jsonObject = new JSONObject();
            if(muzimaSetting != null) {
                String additionalDetailsSettingValue = muzimaSetting.getValueString();
                if (additionalDetailsSettingValue != null) {
                    jsonObject = new JSONObject(additionalDetailsSettingValue);
                    Object object = new JSONObject();
                    String addressColumn = null;
                    List<String> attributeTypeUuids = new ArrayList<>();
                    List<String> identifierTypeUuids = new ArrayList<>();
                    if (jsonObject.has("address")) {
                        object = jsonObject.get("address");
                        PersonAddress personAddress = patient.getPreferredAddress();
                        if (object != null && object instanceof JSONArray) {
                            JSONArray jsonArray = (JSONArray) object;
                            if (personAddress != null) {
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    addressColumn = jsonArray.get(i).toString();
                                    RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    if(addressColumn.equals("address1")){
                                        TextView textViews = new TextView(context);
                                        if(personAddress.getAddress1() != null)
                                            textViews.setText("address1".concat(" : ").concat(personAddress.getAddress1()));
                                        textViews.setId(index);
                                        params1.addRule(RelativeLayout.BELOW, index-1);
                                        holder.additionalDetailsLayout.addView(textViews, params1);
                                        index++;
                                    }else if(addressColumn.equals("address2")){
                                        TextView textViews = new TextView(context);
                                        if(personAddress.getAddress2() != null)
                                            textViews.setText("address2".concat(" : ").concat(personAddress.getAddress2()));
                                        textViews.setId(index);
                                        params1.addRule(RelativeLayout.BELOW, index-1);
                                        holder.additionalDetailsLayout.addView(textViews, params1);
                                        index++;
                                    }else if(addressColumn.equals("address3")){
                                        TextView textViews = new TextView(context);
                                        if(personAddress.getAddress3() != null)
                                            textViews.setText("address3".concat(" : ").concat(personAddress.getAddress3()));
                                        textViews.setId(index);
                                        params1.addRule(RelativeLayout.BELOW, index-1);
                                        holder.additionalDetailsLayout.addView(textViews, params1);
                                        index++;
                                    }else if(addressColumn.equals("address4")){
                                        TextView textViews = new TextView(context);
                                        if(personAddress.getAddress4() != null)
                                            textViews.setText("address4".concat(" : ").concat(personAddress.getAddress4()));
                                        textViews.setId(index);
                                        params1.addRule(RelativeLayout.BELOW, index-1);
                                        holder.additionalDetailsLayout.addView(textViews, params1);
                                        index++;
                                    }else if(addressColumn.equals("address5")){
                                        TextView textViews = new TextView(context);
                                        if(personAddress.getAddress5() != null)
                                            textViews.setText("address5".concat(" : ").concat(personAddress.getAddress5()));
                                        textViews.setId(index);
                                        params1.addRule(RelativeLayout.BELOW, index-1);
                                        holder.additionalDetailsLayout.addView(textViews, params1);
                                        index++;
                                    }else if(addressColumn.equals("address6")){
                                        TextView textViews = new TextView(context);
                                        if(personAddress.getAddress6() != null)
                                            textViews.setText("address6".concat(" : ").concat(personAddress.getAddress6()));
                                        textViews.setId(index);
                                        params1.addRule(RelativeLayout.BELOW, index-1);
                                        holder.additionalDetailsLayout.addView(textViews, params1);
                                        index++;
                                    }else if(addressColumn.equals("cityVillage")){
                                        TextView textViews = new TextView(context);
                                        if(personAddress.getCityVillage() != null)
                                            textViews.setText("City/Village".concat(" : ").concat(personAddress.getCityVillage()));
                                        textViews.setId(index);
                                        params1.addRule(RelativeLayout.BELOW, index-1);
                                        holder.additionalDetailsLayout.addView(textViews, params1);
                                        index++;
                                    }
                                }
                            }
                        }else{
                            addressColumn = object.toString();
                            RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            if(addressColumn.equals("address1")){
                                TextView textViews = new TextView(context);
                                if(personAddress.getAddress1() != null)
                                    textViews.setText("address1".concat(" : ").concat(personAddress.getAddress1()));
                                textViews.setId(index);
                                params1.addRule(RelativeLayout.BELOW, index-1);
                                holder.additionalDetailsLayout.addView(textViews, params1);
                                index++;
                            }else if(addressColumn.equals("address2")){
                                TextView textViews = new TextView(context);
                                if(personAddress.getAddress2() != null)
                                    textViews.setText("address2".concat(" : ").concat(personAddress.getAddress2()));
                                textViews.setId(index);
                                params1.addRule(RelativeLayout.BELOW, index-1);
                                holder.additionalDetailsLayout.addView(textViews, params1);
                                index++;
                            }else if(addressColumn.equals("address3")){
                                TextView textViews = new TextView(context);
                                if(personAddress.getAddress3() != null)
                                    textViews.setText("address3".concat(" : ").concat(personAddress.getAddress3()));
                                textViews.setId(index);
                                params1.addRule(RelativeLayout.BELOW, index-1);
                                holder.additionalDetailsLayout.addView(textViews, params1);
                                index++;
                            }else if(addressColumn.equals("address4")){
                                TextView textViews = new TextView(context);
                                if(personAddress.getAddress4() != null)
                                    textViews.setText("address4".concat(" : ").concat(personAddress.getAddress4()));
                                textViews.setId(index);
                                params1.addRule(RelativeLayout.BELOW, index-1);
                                holder.additionalDetailsLayout.addView(textViews, params1);
                                index++;
                            }else if(addressColumn.equals("address5")){
                                TextView textViews = new TextView(context);
                                if(personAddress.getAddress5() != null)
                                    textViews.setText("address5".concat(" : ").concat(personAddress.getAddress5()));
                                textViews.setId(index);
                                params1.addRule(RelativeLayout.BELOW, index-1);
                                holder.additionalDetailsLayout.addView(textViews, params1);
                                index++;
                            }else if(addressColumn.equals("address6")){
                                TextView textViews = new TextView(context);
                                if(personAddress.getAddress6() != null)
                                    textViews.setText("address6".concat(" : ").concat(personAddress.getAddress6()));
                                textViews.setId(index);
                                params1.addRule(RelativeLayout.BELOW, index-1);
                                holder.additionalDetailsLayout.addView(textViews, params1);
                                index++;
                            }else if(addressColumn.equals("cityVillage")){
                                TextView textViews = new TextView(context);
                                if(personAddress.getCityVillage() != null)
                                    textViews.setText("City/Village".concat(" : ").concat(personAddress.getCityVillage()));
                                textViews.setId(index);
                                params1.addRule(RelativeLayout.BELOW, index-1);
                                holder.additionalDetailsLayout.addView(textViews, params1);
                                index++;
                            }
                        }
                    }

                    if(jsonObject.has("attribute")){
                        object = jsonObject.get("attribute");
                        String uuid = null;
                        List<PersonAttribute> personAttributes = patient.getAtributes();
                        if (object != null && object instanceof JSONArray) {
                            JSONArray jsonArray = (JSONArray) object;
                            for (int i = 0; i < jsonArray.length(); i++) {
                                uuid = jsonArray.get(i).toString();
                                attributeTypeUuids.add(uuid);
                            }

                            for(PersonAttribute personAttribute : personAttributes){
                                if(attributeTypeUuids.contains(personAttribute.getAttributeType().getUuid())) {
                                    RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    TextView textViews = new TextView(context);
                                    textViews.setText(personAttribute.getAttributeType().getName().concat(" : ").concat(personAttribute.getAttribute()));
                                    textViews.setId(index);
                                    params1.addRule(RelativeLayout.BELOW, index-1);
                                    holder.additionalDetailsLayout.addView(textViews, params1);
                                    index++;
                                }
                            }
                        }else{
                            uuid = object.toString();
                            attributeTypeUuids.add(uuid);
                            for(PersonAttribute personAttribute : personAttributes){
                                if(attributeTypeUuids.contains(personAttribute.getAttributeType().getUuid())) {
                                    RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    TextView textViews = new TextView(context);
                                    textViews.setText(personAttribute.getAttributeType().getName().concat(" : ").concat(personAttribute.getAttribute()));
                                    textViews.setId(index);
                                    params1.addRule(RelativeLayout.BELOW, index-1);
                                    holder.additionalDetailsLayout.addView(textViews, params1);
                                    index++;
                                }
                            }
                        }
                    }

                    if(jsonObject.has("identifier")){
                        object = jsonObject.get("identifier");
                        String uuid = null;
                        List<PatientIdentifier>  patientIdentifiers = patient.getIdentifiers();
                        if (object != null && object instanceof JSONArray) {
                            JSONArray jsonArray = (JSONArray) object;
                            for (int i = 0; i < jsonArray.length(); i++) {
                                uuid = jsonArray.get(i).toString();
                                identifierTypeUuids.add(uuid);
                            }
                            for(PatientIdentifier patientIdentifier : patientIdentifiers){
                                if(identifierTypeUuids.contains(patientIdentifier.getIdentifierType().getUuid())) {
                                    RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    TextView textViews = new TextView(context);
                                    textViews.setText(patientIdentifier.getIdentifierType().getName().concat(" : ").concat(patientIdentifier.getIdentifier()));
                                    textViews.setId(index);
                                    params1.addRule(RelativeLayout.BELOW, index-1);
                                    holder.additionalDetailsLayout.addView(textViews, params1);
                                    index++;
                                }
                            }
                        }else{
                            uuid = object.toString();
                            PatientIdentifier patientIdentifier = patient.getIdentifier(uuid);
                            RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            TextView textViews = new TextView(context);
                            textViews.setText(patientIdentifier.getIdentifierType().getName().concat(" : ").concat(patientIdentifier.getIdentifier()));
                            textViews.setId(index);
                            params1.addRule(RelativeLayout.BELOW, index-1);
                            holder.additionalDetailsLayout.addView(textViews, params1);
                            index++;
                        }
                    }
                }
            }

        } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private TextView newTextView(LayoutInflater layoutInflater) {
        TextView textView = (TextView) layoutInflater.inflate(R.layout.tag, null, false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(1, 0, 0, 0);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    protected void onPreExecuteUpdate() {
        patientList.clear();
        if (backgroundListQueryTaskListener != null) {
            backgroundListQueryTaskListener.onQueryTaskStarted();
        }
    }

    protected void onPostExecuteUpdate(List<Patient> patients) {
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
        RelativeLayout additionalDetailsLayout;

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
            additionalDetailsLayout = itemView.findViewById(R.id.patient_additional_details_layout);
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
