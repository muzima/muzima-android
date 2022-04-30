package com.muzima.adapters.observations;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.model.ObsData;
import com.muzima.model.ObsGroups;
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
        MuzimaApplication app = (MuzimaApplication) context.getApplicationContext();
        layoutInflater = LayoutInflater.from(context);
        this.patientUuid = patientUuid;
        this.conceptController = app.getConceptController();
        this.observationController = app.getObservationController();

        h = getHeaders();
        headers = h.toArray(new String[0]);

        obsGroup = new ObsGroups[] {
                //TODO: Add groups from serverSide
                new ObsGroups("Others"),
        };


        density = context.getResources().getDisplayMetrics().density;
        getObservationByConcept();
    }


    public void getObservationByConcept(){
        try {
            concepts = conceptController.getConcepts();
            for(Concept concept : concepts){
                List<String> conceptRow = new ArrayList<>();
                List<Observation> observations  = observationController.getObservationsByPatientuuidAndConceptId(patientUuid,concept.getId());
                for(String dateString : h) {
                    if(dateString.isEmpty()){
                        conceptRow.add(concept.getName());
                    } else {
                        String value = "";
                        for (Observation observation : observations) {
                            if (dateString.equals(dateFormat.format(observation.getObservationDatetime()))) {
                                if(concept.isNumeric()) {
                                    value = String.valueOf(observation.getValueNumeric());
                                } else if (concept.isCoded()){
                                    value = observation.getValueCoded().getName();
                                } else if (concept.isDatetime()){
                                    value = dateFormat.format(observation.getValueDatetime());
                                } else {
                                    value = observation.getValueText();
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
                //TODO: Have correct index for the Concept group
                obsGroup[0].list.add(new ObsData(conceptRow.toArray(new String[0])));
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

        convertView.setBackgroundResource(row % 2 == 0 ? R.drawable.bg_table_color1 : R.drawable.bg_table_color2);
        ((TextView) convertView.findViewById(android.R.id.text1)).setText(getDevice(row).data[column + 1]);
        return convertView;
    }

    private View getBody(int row, int column, View convertView, ViewGroup parent) {
        convertView = layoutInflater.inflate(R.layout.item_table, parent, false);

        convertView.setBackgroundResource(row % 2 == 0 ? R.drawable.bg_table_color1 : R.drawable.bg_table_color2);
        ((TextView) convertView.findViewById(android.R.id.text1)).setText(getDevice(row).data[column + 1]);
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
            height = 30;
        } else if (isGroup(row)) {
            height = 25;
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
