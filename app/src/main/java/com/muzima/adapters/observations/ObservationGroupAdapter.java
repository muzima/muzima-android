/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import static com.muzima.utils.ConceptUtils.getConceptNameFromConceptNamesByLocale;
import static com.muzima.utils.ConceptUtils.getDerivedConceptNameFromConceptNamesByLocale;
import static com.muzima.utils.Constants.FGH.Concepts.HEALTHWORKER_ASSIGNMENT_CONCEPT_ID;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Concept;
import com.muzima.api.model.ConceptName;
import com.muzima.api.model.DerivedConcept;
import com.muzima.api.model.DerivedObservation;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Provider;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.controller.ConceptController;
import com.muzima.controller.DerivedConceptController;
import com.muzima.controller.DerivedObservationController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.model.ObsData;
import com.muzima.model.ObsGroups;
import com.muzima.util.JsonUtils;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ObservationGroupAdapter extends BaseTableAdapter {

    private final ObsGroups obsGroup[];
    List<Concept> concepts = new ArrayList<>();
    List<Concept> conceptWithObservations = new ArrayList<>();
    private final LayoutInflater layoutInflater;
    private final String patientUuid;
    private final ConceptController conceptController;
    private final ObservationController observationController;
    boolean isGroupingEnabled;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    private List<String> h;
    private String headers[];
    List<ObsGroups> obsGroups = new ArrayList<>();
    List<String> groups = new ArrayList<>();
    MuzimaApplication app;
    private  Context context;
    private boolean shouldReplaceProviderIdWithNames;
    Map<String, Integer> map = new LinkedHashMap<>( );
    Map<String, String> conceptUnits = new LinkedHashMap<>( );
    Map<String, String> conceptGroupMap = new LinkedHashMap<>();
    Map<String, List<Observation>> conceptsObservations = new LinkedHashMap<>();
    private final String applicationLanguage;
    private final DerivedConceptController derivedConceptController;
    private final DerivedObservationController derivedObservationController;
    List<DerivedConcept> derivedConcepts = new ArrayList<>();

    public List<String> getHeaders() {
        List<String> dates = new ArrayList();
        dates.add("");
        try {
            List<Observation> observations = observationController.getObservationsByPatient(patientUuid);
            List<DerivedObservation> derivedObservations = derivedObservationController.getDerivedObservationByPatientUuid(patientUuid);
            for (DerivedObservation derivedObservation : derivedObservations) {
                Observation observation = new Observation();
                observation.setObsUuid(derivedObservation.getDerivedObservationUuid());
                observation.setObservationDatetime(derivedObservation.getDateCreated());
                observations.add(observation);
            }

            Collections.sort(observations, obsDateTimeComparator);
            for (Observation observation : observations) {
                if (observation.getObservationDatetime() != null) {
                    String formattedDate = dateFormat.format(observation.getObservationDatetime());
                    if(dates == null){
                        dates.add(formattedDate);
                    } else if(!dates.contains(formattedDate)){
                        dates.add(formattedDate);
                    }
                }
            }
        } catch (ObservationController.LoadObservationException e) {
            Log.w("Observations", String.format("Exception while loading observations for %s."), e);
        } catch (DerivedObservationController.DerivedObservationFetchException e) {
            Log.w(getClass().getSimpleName(), "Exception while loading derived observations for %s.", e);
        }
        return dates;
    }

    private final Comparator<Observation> obsDateTimeComparator = (lhs, rhs) -> {
        if (lhs.getObservationDatetime()==null)
            return -1;
        if (rhs.getObservationDatetime()==null)
            return 1;
        return -(lhs.getObservationDatetime()
                .compareTo(rhs.getObservationDatetime()));
    };

    private final float density;

    public ObservationGroupAdapter(Context context, String patientUuid) {
        this.app = (MuzimaApplication) context.getApplicationContext();
        layoutInflater = LayoutInflater.from(context);
        this.patientUuid = patientUuid;
        this.conceptController = app.getConceptController();
        this.observationController = app.getObservationController();
        this.derivedObservationController = app.getDerivedObservationController();
        this.derivedConceptController = app.getDerivedConceptController();
        this.context = context;
        shouldReplaceProviderIdWithNames = app.getMuzimaSettingController().isPatientTagGenerationEnabled();
        this.applicationLanguage = MuzimaPreferences.getStringPreference(context, context.getResources().getString(R.string.preference_app_language), context.getResources().getString(R.string.language_english));

        h = getHeaders();
        headers = h.toArray(new String[0]);

        List<String> conceptUuids = new ArrayList<>();
        List<Object> objects = null;

        String json = "";
        try {
            SetupConfigurationTemplate activeSetupConfig = app.getSetupConfigurationController().getActiveSetupConfigurationTemplate();
            json = activeSetupConfig.getConfigJson();

            isGroupingEnabled = JsonUtils.readAsBoolean(json, "$['config']['requireConceptGroups']");
            if(isGroupingEnabled) {
                objects = JsonUtils.readAsObjectList(json, "$['config']['configConceptGroups']");
                if (objects != null) {
                    for (Object object : objects) {
                        net.minidev.json.JSONObject configConceptGroups = (net.minidev.json.JSONObject) object;

                        net.minidev.json.JSONObject jsonObject = configConceptGroups;
                        List<Object> concepts = JsonUtils.readAsObjectList(configConceptGroups.toJSONString(), "concepts");
                        Object group = jsonObject.get("name");
                        for (Object concept : concepts) {
                            net.minidev.json.JSONObject concept1 = (net.minidev.json.JSONObject) concept;
                            String conceptUuid = concept1.get("uuid").toString();
                            conceptUuids.add(conceptUuid);

                            Concept cpt = conceptController.getConceptByUuid(conceptUuid);
                            if (cpt != null) {
                                List<Observation> observations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, cpt.getConceptid());
                                if (observations.size() > 0) {
                                    if(!groups.contains(group.toString())) {
                                        obsGroups.add(new ObsGroups(group.toString()));
                                        groups.add(group.toString());
                                    }
                                    conceptsObservations.put(conceptUuid,observations);
                                    conceptGroupMap.put(conceptUuid,group.toString());
                                }
                            }
                        }
                    }
                }
            }

            concepts = conceptController.getConcepts();
            boolean isOtherGroupAdded = false;
            for(Concept concept : concepts){
                if(!conceptUuids.contains(concept.getUuid())){
                    if(!isOtherGroupAdded){
                        if (concept != null) {
                            List<Observation> observations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, concept.getConceptid());
                            if (observations.size() > 0) {
                                if(!groups.contains(app.getString(R.string.general_other))) {
                                    obsGroups.add(new ObsGroups(app.getString(R.string.general_other)));
                                    groups.add(app.getString(R.string.general_other));
                                }
                                conceptGroupMap.put(concept.getUuid(),app.getString(R.string.general_other));
                                conceptsObservations.put(concept.getUuid(),observations);
                            }

                        }
                    }
                }
            }

            derivedConcepts = derivedConceptController.getDerivedConcepts();
            for(DerivedConcept derivedConcept : derivedConcepts){
                if (derivedConcept != null) {
                    List<Observation> observations = new ArrayList<>();
                    List<DerivedObservation> derivedObservations = derivedObservationController.getDerivedObservationByPatientUuidAndDerivedConceptUuid(patientUuid, derivedConcept.getDerivedConceptUuid());
                    for (DerivedObservation derivedObservation : derivedObservations) {
                        Observation observation = new Observation();

                        List<ConceptName> conceptNames = new ArrayList<>();
                        ConceptName conceptName = new ConceptName();
                        conceptName.setName(getDerivedConceptNameFromConceptNamesByLocale(derivedConcept.getDerivedConceptName(), applicationLanguage));
                        conceptName.setLocale(applicationLanguage);
                        conceptNames.add(conceptName);

                        Concept concept = new Concept();
                        concept.setConceptUuid(derivedObservation.getDerivedConcept().getDerivedConceptUuid());
                        concept.setConceptNames(new ArrayList<>(conceptNames));
                        concept.setConceptType(derivedConcept.getConceptType());

                        observation.setObsUuid(derivedObservation.getDerivedObservationUuid());
                        observation.setPerson(derivedObservation.getPerson());
                        observation.setConcept(concept);
                        observation.setValueCoded(derivedObservation.getValueCoded());
                        observation.setValueDatetime(derivedObservation.getValueDatetime());
                        observation.setValueNumeric(derivedObservation.getValueNumeric());
                        observation.setValueText(derivedObservation.getValueText());
                        observation.setValueBoolean(derivedObservation.isValueBoolean());
                        observation.setObservationDatetime(derivedObservation.getDateCreated());

                        observations.add(observation);
                    }

                    if (observations.size() > 0) {
                        if(!groups.contains(app.getString(R.string.general_other))) {
                            obsGroups.add(new ObsGroups(app.getString(R.string.general_other)));
                            groups.add(app.getString(R.string.general_other));
                        }
                        conceptGroupMap.put(derivedConcept.getDerivedConceptUuid(),app.getString(R.string.general_other));
                        conceptsObservations.put(derivedConcept.getDerivedConceptUuid(),observations);
                    }
                }
            }

        } catch (ConceptController.ConceptFetchException | ObservationController.LoadObservationException | SetupConfigurationController.SetupConfigurationFetchException e) {
            Log.e(getClass().getSimpleName(),"Exception encountered while loading Observations or fetching concepts ",e);
        } catch (DerivedConceptController.DerivedConceptFetchException e) {
            Log.e(getClass().getSimpleName(),"Exception encountered while loading derived concepts ",e);
        } catch (DerivedObservationController.DerivedObservationFetchException e) {
            Log.e(getClass().getSimpleName(),"Exception encountered while loading derived observations ",e);
        }

        obsGroup = obsGroups.toArray(new ObsGroups[0]);

        density = context.getResources().getDisplayMetrics().density;
        getObservationByConcept();
    }


    public void getObservationByConcept(){
        try {
            List<String> conceptUuids = new ArrayList<>();

            for (Map.Entry<String,List<Observation>> pair : conceptsObservations.entrySet()){
                Concept concept = conceptController.getConceptByUuid(pair.getKey());
                conceptUuids.add(pair.getKey());
                if (concept != null) {
                    List<String> conceptRow = new ArrayList<>();
                    List<Observation> observations = pair.getValue();
                    if (observations.size() > 0) {
                        conceptWithObservations.add(concept);
                        for (String dateString : h) {
                            if (dateString.isEmpty()) {
                                conceptRow.add(getConceptNameFromConceptNamesByLocale(concept.getConceptNames(), applicationLanguage));
                            } else {
                                String value = "";
                                for (Observation observation : observations) {
                                    if (dateString.equals(dateFormat.format(observation.getObservationDatetime()))) {
                                        if (shouldReplaceProviderIdWithNames && observation.getConcept().getConceptid() == HEALTHWORKER_ASSIGNMENT_CONCEPT_ID) {
                                            Provider provider = app.getProviderController().getProviderBySystemId(observation.getValueText());
                                            if (provider != null) {
                                                value = provider.getName();
                                            } else {
                                                value = observation.getValueText();
                                            }
                                        } else {
                                            if (concept.isNumeric()) {
                                                value = String.valueOf(observation.getValueNumeric());
                                            } else if (concept.isCoded()) {
                                                value = getConceptNameFromConceptNamesByLocale(observation.getValueCoded().getConceptNames(), applicationLanguage);
                                            } else if (concept.isDatetime()) {
                                                if(observation.getValueDatetime() != null)
                                                    value = dateFormat.format(observation.getValueDatetime());
                                            } else {
                                                value = observation.getValueText();
                                            }
                                        }
                                    }
                                }
                                if (!StringUtils.isEmpty(value)) {
                                    conceptRow.add(value);
                                } else {
                                    conceptRow.add("");
                                }
                            }
                        }
                        String units = "";
                        if(!StringUtils.isEmpty(concept.getUnit())){
                            units = concept.getUnit();
                        }

                        map.put(getConceptNameFromConceptNamesByLocale(concept.getConceptNames(), applicationLanguage), groups.indexOf(conceptGroupMap.get(pair.getKey())));
                        conceptUnits.put(getConceptNameFromConceptNamesByLocale(concept.getConceptNames(), applicationLanguage), units);
                        obsGroup[groups.indexOf(conceptGroupMap.get(pair.getKey()))].list.add(new ObsData(conceptRow.toArray(new String[0])));
                    }
                }else {
                    DerivedConcept derivedConcept = derivedConceptController.getDerivedConceptByUuid(pair.getKey());
                    if (derivedConcept != null) {
                        List<String> conceptRow = new ArrayList<>();
                        List<Observation> observations = pair.getValue();
                        if (observations.size() > 0) {
                            conceptWithObservations.add(observations.get(0).getConcept());
                            for (String dateString : h) {
                                if (dateString.isEmpty()) {
                                    conceptRow.add(getConceptNameFromConceptNamesByLocale(observations.get(0).getConcept().getConceptNames(), applicationLanguage));
                                } else {
                                    String value = "";
                                    for (Observation observation : observations) {
                                        if (dateString.equals(dateFormat.format(observation.getObservationDatetime()))) {
                                            if (shouldReplaceProviderIdWithNames && observation.getConcept().getConceptid() == HEALTHWORKER_ASSIGNMENT_CONCEPT_ID) {
                                                Provider provider = app.getProviderController().getProviderBySystemId(observation.getValueText());
                                                if (provider != null) {
                                                    value = provider.getName();
                                                } else {
                                                    value = observation.getValueText();
                                                }
                                            } else {
                                                if (derivedConcept.isNumeric()) {
                                                    value = String.valueOf(observation.getValueNumeric());
                                                } else if (derivedConcept.isCoded()) {
                                                    value = getConceptNameFromConceptNamesByLocale(observation.getValueCoded().getConceptNames(), applicationLanguage);
                                                } else if (derivedConcept.isDatetime()) {
                                                    if (observation.getValueDatetime() != null)
                                                        value = dateFormat.format(observation.getValueDatetime());
                                                } else if(derivedConcept.isBoolean()){
                                                    value = String.valueOf(observation.isValueBoolean());
                                                } else {
                                                    value = observation.getValueText();
                                                }
                                            }
                                        }
                                    }
                                    if (!StringUtils.isEmpty(value)) {
                                        conceptRow.add(value);
                                    } else {
                                        conceptRow.add("");
                                    }
                                }
                            }
                            String units = "";
                            map.put(getDerivedConceptNameFromConceptNamesByLocale(derivedConcept.getDerivedConceptName(), applicationLanguage), groups.indexOf(conceptGroupMap.get(pair.getKey())));
                            conceptUnits.put(getDerivedConceptNameFromConceptNamesByLocale(derivedConcept.getDerivedConceptName(), applicationLanguage), units);
                            obsGroup[groups.indexOf(conceptGroupMap.get(pair.getKey()))].list.add(new ObsData(conceptRow.toArray(new String[0])));
                        }
                    }
                }
            }
        } catch (ConceptController.ConceptFetchException e) {
            Log.e(getClass().getSimpleName(),"Exception encountered while fetching concepts ",e);
        } catch (DerivedConceptController.DerivedConceptFetchException e) {
            Log.e(getClass().getSimpleName(),"Exception encountered while getting derived concepts ",e);
        }
    }

    @Override
    public int getRowCount() {
        return conceptWithObservations.size()+obsGroup.length;
    }

    @Override
    public int getColumnCount() {
        return h.size()-1;
    }

    @Override
    public View getView(int row, int column, View convertView, ViewGroup parent) {
        final View view;
        switch (getItemViewType(row, column)) {
            case 0:
                view = getFirstHeader(row, column, convertView, parent);
                break;
            case 1:
                view = getHeader(row, column, convertView, parent);
                break;
            case 2:
                view = getFirstBody(row, column, convertView, parent);
                break;
            case 3:
                view = getBody(row, column, convertView, parent);
                break;
            case 4:
                view = getGroupView(row, column, convertView, parent);
                break;
            default:
                throw new RuntimeException("Not sure what went wrong");
        }
        return view;
    }

    private View getFirstHeader(int row, int column, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_table_header_first, parent, false);

        }
        ((TextView) convertView.findViewById(R.id.text1)).setText(headers[0]);
        return convertView;
    }

    private View getHeader(int row, int column, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_table_header, parent, false);
        }
        ((TextView) convertView.findViewById(R.id.text1)).setText(headers[column + 1]);

        return convertView;
    }

    private View getFirstBody(int row, int column, View convertView, ViewGroup parent) {

        convertView = layoutInflater.inflate(R.layout.item_table_first, parent, false);
        ((TextView) convertView.findViewById(android.R.id.text1)).setBackgroundResource(R.drawable.bg_table_color1);

        ((TextView) convertView.findViewById(android.R.id.text1)).setText(getDevice(row).data[column + 1]);

        try {
            Concept concept = conceptController.getConceptByName(getDevice(row).data[column + 1]);
            if(concept!=null) {
                setClickListenersOnView(getDevice(row).data[column + 1], convertView);
            }
        } catch (ConceptController.ConceptFetchException e) {
            Log.e(getClass().getSimpleName(),"Error while loading concepts");
        }

        return convertView;
    }


    private void setClickListenersOnView(String concept, View convertView) {
        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showDialog(concept);
                return true;
            }
        });
    }

    public void showDialog(String conceptName){
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        androidx.appcompat.app.AlertDialog.Builder addIndividualObservationsDialogBuilder =
                new androidx.appcompat.app.AlertDialog.Builder(
                        context
                );

        View conceptDetails = layoutInflater.inflate(R.layout.obs_by_concept_details, null);
        addIndividualObservationsDialogBuilder.setView(conceptDetails);
        addIndividualObservationsDialogBuilder
                .setCancelable(true);

        androidx.appcompat.app.AlertDialog obsDetailsViewDialog = addIndividualObservationsDialogBuilder.create();
        obsDetailsViewDialog.show();

        TextView conceptTextView = conceptDetails.findViewById(R.id.concept_name);
        conceptTextView.setText(conceptName);

        List<Observation> observations = new ArrayList<>();
        Concept concept = new Concept();
        try {
            concept = conceptController.getConceptByName(conceptName);
            observations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid,concept.getConceptid());
        } catch (ConceptController.ConceptFetchException | ObservationController.LoadObservationException e) {
            Log.e(getClass().getSimpleName(), "Encountered an error while getting concept or observations");
        }

        RecyclerView recyclerView = conceptDetails.findViewById(R.id.recyclerView);;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new ObsListAdapter(new ArrayList<>(observations),context,shouldReplaceProviderIdWithNames, concept, app, applicationLanguage));
    }

    private View getBody(int row, int column, View convertView, ViewGroup parent) {
        convertView = layoutInflater.inflate(R.layout.item_table, parent, false);

        ((TextView) convertView.findViewById(android.R.id.text1)).setText(getDevice(row).data[column + 1]);
        if(!StringUtils.isEmpty(getDevice(row).data[column + 1])) {
            ((TextView) convertView.findViewById(android.R.id.text2)).setText(conceptUnits.get(getDevice(row).data[0]));
        }
        ((LinearLayout) convertView.findViewById(R.id.ll1)).setBackgroundResource(R.drawable.table_border1);
        return convertView;
    }

    private View getGroupView(int row, int column, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_table_group, parent, false);
        }
        final String string;
        if (column == -1) {
            string = getGroup(row).name;
        } else {
            string = "";
        }

        ((TextView) convertView.findViewById(android.R.id.text1)).setText(string);
        convertView.setBackgroundResource(R.drawable.bg_table_color2);
        return convertView;
    }

    @Override
    public int getWidth(int column) {
        return Math.round(120 * density);
    }

    @Override
    public int getHeight(int row) {
        final int height;
        if (row == -1) {
            if(conceptWithObservations.size() == 0)
                height = 0;
            else
                height = 40;
        } else if (isGroup(row)) {
            if(!isGroupingEnabled) {
                height = 0;
            }else{
                height = 40;
            }
        } else {
            height = 60;
        }
        return Math.round(height * density);
    }

    @Override
    public int getItemViewType(int row, int column) {
        final int itemViewType;
        if (row == -1 && column == -1) {
            itemViewType = 0;
        } else if (row == -1) {
            itemViewType = 1;
        } else if (isGroup(row)) {
            itemViewType = 4;
        } else if (column == -1) {
            itemViewType = 2;
        } else {
            itemViewType = 3;
        }
        return itemViewType;
    }

    private boolean isGroup(int row) {
        int group = 0;
        while (row > 0) {
            row -= obsGroup[group].size() + 1;
            group++;
        }
        return row == 0;
    }

    private ObsGroups getGroup(int row) {
        int group = 0;
        while (row >= 0) {
            row -= obsGroup[group].size() + 1;
            group++;
        }
        return obsGroup[group - 1];
    }

    private ObsData getDevice(int row) {
        int group = 0;
        while (row >= 0) {
            row -= obsGroup[group].size() + 1;
            group++;
        }
        group--;
        return obsGroup[group].get(row + obsGroup[group].size());
    }

    @Override
    public int getViewTypeCount() {
        return 5;
    }
}
