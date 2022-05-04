package com.muzima.adapters.observations;

import static com.muzima.utils.ConceptUtils.getConceptNameFromConceptNamesByLocale;
import static com.muzima.utils.Constants.FGH.Concepts.HEALTHWORKER_ASSIGNMENT_CONCEPT_ID;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Provider;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.model.ObsData;
import com.muzima.model.ObsGroups;
import com.muzima.util.JsonUtils;
import com.muzima.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ObservationGroupAdapter extends BaseTableAdapter {

    private final ObsGroups obsGroup[];
    List<Concept> concepts = new ArrayList<>();
    private final LayoutInflater layoutInflater;
    private final String patientUuid;
    private final ConceptController conceptController;
    private final ObservationController observationController;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    private List<String> h;
    private String headers[];
    List<ObsGroups> obsGroups = new ArrayList<>();
    List<String> groups = new ArrayList<>();
    MuzimaApplication app;
    private  Context context;
    private boolean shouldReplaceProviderIdWithNames;
    int groupNumber = 0;

    public List<String> getHeaders() {
        List<String> dates = new ArrayList();
        dates.add("");
        try {
            List<Observation> observations = observationController.getObservationsByPatient(patientUuid);
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
        this.context = context;
        shouldReplaceProviderIdWithNames = app.getMuzimaSettingController().isPatientTagGenerationEnabled();

        h = getHeaders();
        headers = h.toArray(new String[0]);

        List<String> conceptUuids = new ArrayList<>();

        String json = "";
        try {
            SetupConfigurationTemplate activeSetupConfig = app.getSetupConfigurationController().getActiveSetupConfigurationTemplate();
            json = activeSetupConfig.getConfigJson();
        } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
            e.printStackTrace();
        }

        List<Object> objects = JsonUtils.readAsObjectList(json, "$['config']['configConceptGroups']");
        if (objects != null) {
            for (Object object : objects) {
                net.minidev.json.JSONObject configConceptGroups = (net.minidev.json.JSONObject) object;

                net.minidev.json.JSONObject jsonObject = configConceptGroups;
                List<Object> concepts = JsonUtils.readAsObjectList(configConceptGroups.toJSONString(), "concepts");
                Object group = jsonObject.get("name");
                obsGroups.add(new ObsGroups(group.toString()));
                groups.add(group.toString());
                for (Object concept : concepts) {
                    net.minidev.json.JSONObject concept1 = (net.minidev.json.JSONObject) concept;
                    String conceptUuid = concept1.get("uuid").toString();
                    conceptUuids.add(conceptUuid);
                }
            }
        }

        try {
            concepts = conceptController.getConcepts();
            boolean isOtherGroupAdded = false;
            for(Concept concept : concepts){
                if(!conceptUuids.contains(concept.getUuid())){
                    if(!isOtherGroupAdded){
                        obsGroups.add(new ObsGroups(app.getString(R.string.general_other)));
                        groups.add(app.getString(R.string.general_other));
                        break;
                    }
                }
            }
        } catch (ConceptController.ConceptFetchException e) {
            e.printStackTrace();
        }

        obsGroup = obsGroups.toArray(new ObsGroups[0]);


        density = context.getResources().getDisplayMetrics().density;
        getObservationByConcept(objects);
    }


    public void getObservationByConcept(List<Object> objects){
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            String applicationLanguage = preferences.getString(context.getResources().getString(R.string.preference_app_language), context.getResources().getString(R.string.language_english));

            List<String> conceptUuids = new ArrayList<>();
            if (objects != null) {
                for (Object object : objects) {
                    net.minidev.json.JSONObject configConceptGroups = (net.minidev.json.JSONObject) object;

                    net.minidev.json.JSONObject jsonObject = configConceptGroups;
                    List<Object> concepts = JsonUtils.readAsObjectList(configConceptGroups.toJSONString(), "concepts");
                    for (Object conceptObject : concepts) {
                        net.minidev.json.JSONObject concept1 = (net.minidev.json.JSONObject) conceptObject;
                        String conceptUuid = concept1.get("uuid").toString();
                        Concept concept = conceptController.getConceptByUuid(conceptUuid);
                        conceptUuids.add(conceptUuid);
                        if (concept != null){
                            List<String> conceptRow = new ArrayList<>();
                            List<Observation> observations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, concept.getId());
                            for (String dateString : h) {
                                if (dateString.isEmpty()) {
                                    conceptRow.add(getConceptNameFromConceptNamesByLocale(concept.getConceptNames(),applicationLanguage));
                                } else {
                                    String value = "";
                                    for (Observation observation : observations) {
                                        if (dateString.equals(dateFormat.format(observation.getObservationDatetime()))) {
                                            if(shouldReplaceProviderIdWithNames && observation.getConcept().getId() == HEALTHWORKER_ASSIGNMENT_CONCEPT_ID){
                                                Provider provider = app.getProviderController().getProviderBySystemId(observation.getValueText());
                                                if(provider != null){
                                                    value = provider.getName();
                                                } else {
                                                    value = observation.getValueText();
                                                }
                                            }else {
                                                if (concept.isNumeric()) {
                                                    value = String.valueOf(observation.getValueNumeric());
                                                } else if (concept.isCoded()) {
                                                    value = getConceptNameFromConceptNamesByLocale(observation.getValueCoded().getConceptNames(), applicationLanguage);
                                                } else if (concept.isDatetime()) {
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
                            obsGroup[groups.indexOf(jsonObject.get("name"))].list.add(new ObsData(conceptRow.toArray(new String[0])));
                        }
                    }
                }
            }


            concepts = conceptController.getConcepts();
            for(Concept concept : concepts) {
                if(!conceptUuids.contains(concept.getUuid())){
                    List<String> conceptRow = new ArrayList<>();
                    List<Observation> observations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, concept.getId());
                    for (String dateString : h) {
                        if (dateString.isEmpty()) {
                            conceptRow.add(concept.getName());
                        } else {
                            String value = "";
                            for (Observation observation : observations) {
                                if (dateString.equals(dateFormat.format(observation.getObservationDatetime()))) {
                                    if(shouldReplaceProviderIdWithNames && observation.getConcept().getId() == HEALTHWORKER_ASSIGNMENT_CONCEPT_ID){
                                        Provider provider = app.getProviderController().getProviderBySystemId(observation.getValueText());
                                        if(provider != null){
                                            value = provider.getName();
                                        } else {
                                            value = observation.getValueText();
                                        }
                                    }else {
                                        if (concept.isNumeric()) {
                                            value = String.valueOf(observation.getValueNumeric());
                                        } else if (concept.isCoded()) {
                                            value = getConceptNameFromConceptNamesByLocale(observation.getValueCoded().getConceptNames(), applicationLanguage);
                                        } else if (concept.isDatetime()) {
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
                    obsGroup[groups.indexOf(app.getString(R.string.general_other))].list.add(new ObsData(conceptRow.toArray(new String[0])));
                }
            }
        } catch (ConceptController.ConceptFetchException | ObservationController.LoadObservationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getRowCount() {
        return concepts.size()+obsGroup.length;
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

        convertView.setBackgroundResource(groupNumber % 2 == 0 ? R.drawable.bg_table_color1 : R.drawable.bg_table_color2);
        ((TextView) convertView.findViewById(android.R.id.text1)).setText(getDevice(row).data[column + 1]);
        return convertView;
    }

    private View getBody(int row, int column, View convertView, ViewGroup parent) {
        convertView = layoutInflater.inflate(R.layout.item_table, parent, false);

        ((TextView) convertView.findViewById(android.R.id.text1)).setText(getDevice(row).data[column + 1]);
        ((TextView) convertView.findViewById(android.R.id.text1)).setBackgroundResource(groupNumber % 2 == 0 ? R.drawable.table_border1 : R.drawable.table_border2);

        return convertView;
    }

    private View getGroupView(int row, int column, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_table_group, parent, false);
        }
        final String string;
        if (column == -1) {
            string = getGroup(row).name;
            groupNumber++;
        } else {
            string = "";
        }

        ((TextView) convertView.findViewById(android.R.id.text1)).setText(string);
        convertView.setBackgroundResource(groupNumber % 2 == 0 ? R.drawable.bg_table_color1 : R.drawable.bg_table_color2);
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
            height = 40;
        } else if (isGroup(row)) {
            height = 40;
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
