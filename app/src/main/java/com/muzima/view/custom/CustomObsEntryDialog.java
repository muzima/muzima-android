package com.muzima.view.custom;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.adapters.observations.ObservationsPagerAdapter;
import com.muzima.api.model.Concept;
import com.muzima.api.model.ConceptType;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.EncounterType;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Provider;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;


import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.MuzimaApplication;
import com.muzima.utils.IndividualObsJsonMapper;
import com.muzima.view.observations.ObservationsActivity;
import com.muzima.view.patients.PatientSummaryActivity;

import org.json.JSONException;

public class CustomObsEntryDialog extends Dialog {

    private Button saveEnteredObsValueButton;
    private EditText observationValueEditText;
    private TextView encounterDateTextView;
    private String enteredObservationValue;
    private Concept concept;
    private Provider provider;
    private Observation observation;
    private Encounter encounter;
    private Calendar obsRecordDate = Calendar.getInstance();
    private static final Calendar today = Calendar.getInstance();
    private ObservationController observationController;
    private TextView observation_name_textview;
    private MuzimaApplication muzimaApplication;
    private Spinner codedObsSpinner;
    private Patient patient;
    private  Context context;
    private FormData formData;
    private FormController formController;
    private TextView dateTimeValueEditText;
    private Button cancelDialogButton;

    public CustomObsEntryDialog(@NonNull Context context, MuzimaApplication muzimaApplication, Patient patient) {
        super(context);
        this.muzimaApplication = muzimaApplication;
        this.observationController = muzimaApplication.getObservationController();
        this.formController = muzimaApplication.getFormController();
        this.patient = patient;
        this.context = context;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.add_individual_obs_dialog_layout);

        saveEnteredObsValueButton = (Button) findViewById(R.id.add_new_obs_button);
        cancelDialogButton = (Button) findViewById(R.id.cancel_dialog_button);
        observationValueEditText = (EditText) findViewById(R.id.obs_new_value_edittext);
        encounterDateTextView = (TextView) findViewById(R.id.date_value_textview);
        dateTimeValueEditText = (TextView) findViewById(R.id.obs_new_value_date);
        observation_name_textview = (TextView) findViewById(R.id.observation_name_textview);
        codedObsSpinner = (Spinner) findViewById(R.id.obs_new_value_spinner);

        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Date date = new Date();
        encounterDateTextView.setText(dateFormat.format(date));


        if (concept != null) {
            observation_name_textview.setText(concept.getName());
            //Determine dialog layout based on concept type.
            if (concept.isCoded()) {
                codedObsSpinner.setVisibility(View.VISIBLE);
                observationValueEditText.setVisibility(View.GONE);
                dateTimeValueEditText.setVisibility(View.GONE);
            }

            if (concept.isDatetime()) {
                dateTimeValueEditText.setVisibility(View.VISIBLE);
                dateTimeValueEditText.setText(dateFormat.format(date));
                codedObsSpinner.setVisibility(View.GONE);
                observationValueEditText.setVisibility(View.GONE);
                encounterDateTextView.setInputType(InputType.TYPE_CLASS_DATETIME);
            }

            if (concept.isNumeric()) {
                observationValueEditText.setVisibility(View.VISIBLE);
                codedObsSpinner.setVisibility(View.GONE);
                dateTimeValueEditText.setVisibility(View.GONE);
                observationValueEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
            }

            if (!concept.isNumeric() && !concept.isDatetime() && !concept.isCoded()){
                observationValueEditText.setVisibility(View.VISIBLE);
            }
        }

        cancelDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
            }
        });

        saveEnteredObsValueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean conceptIsDouble = false;
                if(concept.isNumeric()){
                    try  {
                         Double.valueOf(observationValueEditText.getText().toString());
                         conceptIsDouble = true;
                    } catch (NumberFormatException e){
                         conceptIsDouble = false;
                    }
                }
                if((!concept.isCoded() && !concept.isDatetime()) && (observationValueEditText.getText().toString()).isEmpty()) {
                    Toast.makeText(getContext( ),  concept.getName()+" Observation value is needed ", Toast.LENGTH_SHORT).show();
                } else if(concept.isNumeric() && !conceptIsDouble){
                    Toast.makeText(getContext( ),  concept.getName()+" must be a number ", Toast.LENGTH_SHORT).show();
                } else {
                    Observation newObs = constructObservation(concept);

                    if (newObs != null && newObs.getConcept( ).getUuid( ) != null) {
                        try {
                            List<Observation> obsList = new ArrayList<>( );
                            obsList.add(newObs);
                            observationController.saveObservations(obsList);

                            IndividualObsJsonMapper individualObsJsonMapper = new IndividualObsJsonMapper(newObs, patient, muzimaApplication);
                            try {
                                formData = individualObsJsonMapper.createFormDataFromObservation( );
                                formController.saveFormData(formData);
                            } catch (JSONException e) {
                                Log.e("", "Error While Parsing data" + e);
                            } catch (FormController.FormDataSaveException e) {
                                Log.e("", "Error While Saving Form Data" + e);
                            }

                            Toast.makeText(getContext( ), concept.getName( ) + " observation saved succesfully.", Toast.LENGTH_SHORT).show( );
                            Intent intent = new Intent(context,ObservationsActivity.class);
                            intent.putExtra(PatientSummaryActivity.PATIENT,patient);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            context.startActivity(intent);
                            cancel( );
                        } catch (ObservationController.SaveObservationException e) {
                            Log.e("","Error While Saving Observations");
                        }

                    } else {
                        Toast.makeText(getContext( ), "Unfortunately, mUzima could not save observation.", Toast.LENGTH_LONG).show( );
                    }
                }


            }
        });

        encounterDateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicketDialog(v);
            }
        });

        dateTimeValueEditText.setOnClickListener(new View.OnClickListener( ) {
            @Override
            public void onClick(View v) {
                showDatePicketDialog(v);
            }
        });

    }

    public void showDatePicketDialog(View view) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext(), new DateSetListener(), today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        encounterDateTextView = (TextView) view;
        datePickerDialog.show();
    }

    private Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public void setConcept(Concept concept) {
        this.concept = concept;
    }

    private Encounter constructEncounter() {
        return encounter;
    }

    private Concept defaultValueCodedConcept() {
        Concept valueCoded = new Concept();
        valueCoded.setConceptType(new ConceptType());
        return valueCoded;
    }

    private EncounterType getDummyEncounterType() {
        EncounterType encounterType = new EncounterType();
        encounterType.setUuid("encounterTypeForObservationsCreatedOnPhone");
        encounterType.setName("encounterTypeForObservationsCreatedOnPhone");
        return encounterType;
    }

    private Observation constructObservation(Concept concept) {
        Observation newObservation = new Observation();
        Encounter encounter = new Encounter();
        EncounterType encounterType = getDummyEncounterType();
        Date encounterDateTime = parseDateTime(encounterDateTextView.getText().toString());

        encounter.setEncounterDatetime(encounterDateTime);
        encounter.setEncounterType(encounterType);
        encounter.setUuid(UUID.randomUUID().toString());

        newObservation.setConcept(concept);
        newObservation.setUuid(UUID.randomUUID().toString()+new Random().nextInt());
        newObservation.setEncounter(encounter);
        newObservation.setValueCoded(defaultValueCodedConcept());
        newObservation.setPerson(patient);
        newObservation.setObservationDatetime(obsRecordDate.getTime());

        if (concept.isNumeric()){
            newObservation.setValueNumeric(Double.valueOf(observationValueEditText.getText().toString()));
        }

        if (concept.isDatetime()){
            newObservation.setValueDatetime(parseDateTime(dateTimeValueEditText.getText().toString()));
        }

        if (concept.isCoded()){
            //TODO Enable coded individual obs after fixing MUZIMA-620
        }

        if (!concept.isNumeric() && !concept.isDatetime() && !concept.isCoded()){
            newObservation.setValueText(observationValueEditText.getText().toString());
        }

        return newObservation;
    }

    public Date parseDateTime(String dateTime){
        try {
            DateFormat dateTimeFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            if(dateTime.length()<=10){
                dateTime = dateTime.concat(" 00:00");
            }
            return  dateTimeFormat.parse(dateTime);
        }  catch (ParseException e) {
            Log.e(getClass().getSimpleName(), "Error while parsing response JSON", e);
        }
        return null;
    }

    private void setEnteredObservationValue(String enteredObservationValue) {
        this.enteredObservationValue = enteredObservationValue;
    }

    private void setObservation(Observation observation) {
        this.observation = observation;
    }

    private void setEncounter(Encounter encounter) {
        this.encounter = encounter;
    }

    private void setObsRecordDate(Calendar obsRecordDate) {
        this.obsRecordDate = obsRecordDate;
    }

    public class DateSetListener implements DatePickerDialog.OnDateSetListener {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            monthOfYear = monthOfYear+1;
            String month = ""+monthOfYear;
            if(monthOfYear<10){
                month = "0"+monthOfYear;
            }
            String day = ""+dayOfMonth;
            if(dayOfMonth<10){
                day = "0"+dayOfMonth;
            }
            obsRecordDate.set(Calendar.YEAR, year);
            obsRecordDate.set(Calendar.MONTH, monthOfYear);
            obsRecordDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            setObsRecordDate(obsRecordDate);
            encounterDateTextView.setText(day+"-"+month+"-"+year);
        }
    }

}
