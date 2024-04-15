package com.muzima.view.htc;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.HTCPerson;
import com.muzima.api.model.Location;
import com.muzima.api.model.MuzimaHtcForm;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.Patient;
import com.muzima.controller.HTCPersonController;
import com.muzima.controller.LocationController;
import com.muzima.controller.MuzimaHTCFormController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.model.patient.PatientItem;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ViewUtil;
import com.muzima.view.main.HTCMainActivity;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.muzima.util.Constants.ServerSettings.DEFAULT_ENCOUNTER_LOCATION_SETTING;

import com.muzima.utils.Constants;

public class HTCFormActivity extends AppCompatActivity {
    private ImageButton identificationDataBtn;
    private ImageButton bookDataSectionBtn;
    private ImageButton atsTestingDataSectionBtn;
    private ImageButton historicalTestingSectionBtn;
    private ImageButton additionalInformationSectionBtn;
    private LinearLayout identificationDataLyt;
    private LinearLayout bookDataSectionLyt;
    private LinearLayout atsTestingDataSectionLyt;
    private LinearLayout historicalTestingSectionLyt;
    private LinearLayout additionalInformationLyt;
    private boolean identificationSection;
    private boolean bookDataSection;
    private boolean atsTestingDataSection;
    private boolean historicalTestingSection;
    private boolean additionalInformationSection;
    ImageView genderImg;
    TextView name;
    TextView identifier;
    TextView age;
    TextView bookNumber;
    TextView bookPageNumber;
    TextView bookPageLine;
    Spinner testingSectors;
    static EditText testingDate;
    Spinner popKeysMiners;
    Spinner indexCaseContacts;
    TextView testResult;
    CheckBox selfTestConfirmation;
    RadioButton firstTimeTestedOption;
    RadioButton pastPositiveOption;
    EditText dateOfCreation;
    EditText healthFacility;
    ImageView saveHtcForm;
    private Date dateOfTesting;
    private HTCPerson htcPerson;
    private MuzimaHtcForm htcForm;
    private boolean isEditionFlow;
    private boolean isAddATSForSESPExistingPerson;
    private List<PatientItem> searchResults;
    private HTCPersonController htcPersonController;
    private MuzimaHTCFormController htcFormController;
    Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_htcform);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.htc_data_register);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        initViews();
        initController();
        setListners();
        isAddATSForSESPExistingPerson = (Boolean) getIntent().getSerializableExtra("isAddATSForSESPExistingPerson");
        searchResults = (List<PatientItem>) getIntent().getSerializableExtra("searchResults");
        htcPerson = (HTCPerson) getIntent().getSerializableExtra("htcPerson");
        if(htcPerson.isPatient()) {
            identifier.setText(htcPerson.getSespUuid());
        }
        isEditionFlow = (Boolean) getIntent().getSerializableExtra("isEditionFlow");
        if(isEditionFlow) {
            htcForm = htcFormController.getHTCFormByHTCPersonUuid(htcPerson.getUuid());
        }
        setHtcPersonIdentificationData(htcPerson);
        testResult.setText(setHivResult(htcPerson));
        testResult.setEnabled(false);
        setHTCFormData();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setListners() {
        setLayoutControlListners();
    }

    private void setLayoutControlListners() {
        identificationDataBtn.setOnClickListener(view -> {
            identificationSection = !identificationSection;
            if (identificationSection) {
                identificationDataLyt.setVisibility(View.VISIBLE);
                ViewUtil.collapse(identificationDataLyt);
                identificationDataBtn.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            } else {
                identificationDataLyt.setVisibility(View.GONE);
                ViewUtil.expand(identificationDataLyt);
                identificationDataBtn.setImageResource(R.drawable.sharp_arrow_drop_up_24);
            }
        });
        bookDataSectionBtn.setOnClickListener(view -> {
            bookDataSection = !bookDataSection;
            if (bookDataSection) {
                bookDataSectionLyt.setVisibility(View.VISIBLE);
                ViewUtil.collapse(bookDataSectionLyt);
                bookDataSectionBtn.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            } else {
                bookDataSectionLyt.setVisibility(View.GONE);
                ViewUtil.expand(bookDataSectionLyt);
                bookDataSectionBtn.setImageResource(R.drawable.sharp_arrow_drop_up_24);
            }
        });
        atsTestingDataSectionBtn.setOnClickListener(view -> {
            atsTestingDataSection = !atsTestingDataSection;
            if (atsTestingDataSection) {
                atsTestingDataSectionLyt.setVisibility(View.VISIBLE);
                ViewUtil.collapse(atsTestingDataSectionLyt);
                atsTestingDataSectionBtn.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            } else {
                atsTestingDataSectionLyt.setVisibility(View.GONE);
                ViewUtil.expand(atsTestingDataSectionLyt);
                atsTestingDataSectionBtn.setImageResource(R.drawable.sharp_arrow_drop_up_24);
            }
        });
        historicalTestingSectionBtn.setOnClickListener(view -> {
            historicalTestingSection = !historicalTestingSection;
            if (historicalTestingSection) {
                historicalTestingSectionLyt.setVisibility(View.VISIBLE);
                ViewUtil.collapse(historicalTestingSectionLyt);
                historicalTestingSectionBtn.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            } else {
                historicalTestingSectionLyt.setVisibility(View.GONE);
                ViewUtil.expand(historicalTestingSectionLyt);
                historicalTestingSectionBtn.setImageResource(R.drawable.sharp_arrow_drop_up_24);
            }
        });
        additionalInformationSectionBtn.setOnClickListener(view -> {
            additionalInformationSection = !additionalInformationSection;
            if (additionalInformationSection) {
                additionalInformationLyt.setVisibility(View.VISIBLE);
                ViewUtil.collapse(additionalInformationLyt);
                additionalInformationSectionBtn.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            }
            else {
                additionalInformationLyt.setVisibility(View.GONE);
                ViewUtil.expand(additionalInformationLyt);
                additionalInformationSectionBtn.setImageResource(R.drawable.sharp_arrow_drop_up_24);
            }
        });

        testingDate.setOnClickListener(view -> {
                    int mYear, mMonth, mDay;

                    final Calendar c = Calendar.getInstance();
                    mYear = c.get(Calendar.YEAR);
                    mMonth = c.get(Calendar.MONTH);
                    mDay = c.get(Calendar.DAY_OF_MONTH);
                    DatePickerDialog datePickerDialog = new DatePickerDialog(HTCFormActivity.this, new DatePickerDialog.OnDateSetListener() {

                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            testingDate.setText(DateUtils.getFormattedDate(DateUtils.createDate(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year, DateUtils.SIMPLE_DAY_MONTH_YEAR_DATE_FORMAT), DateUtils.SIMPLE_DAY_MONTH_YEAR_DATE_FORMAT));
                        }
                    }, mYear, mMonth, mDay);
                    datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
                    datePickerDialog.show();
        });

        saveHtcForm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(validateFields()) {
                    try {
                        if(isEditionFlow) {
                            htcPersonController.updateHTCPerson(htcPerson);
                            MuzimaHtcForm muzimaHtcForm = createHTCFormInstance(htcPerson);
                            htcFormController.updateHTCForm(muzimaHtcForm);
                            int i=0;
                            for (PatientItem patientItem : searchResults) {
                                if(patientItem.getPatient().getUuid().equalsIgnoreCase(htcPerson.getUuid())) {
                                    break;
                                }
                                i++;
                            }
                            searchResults.add(i, new PatientItem(htcPerson));
                            ViewUtil.displayAlertDialog(HTCFormActivity.this,getResources().getString(R.string.record_updated_successful)).show();
                            goToMainActivity(searchResults);
                        } else {
                            htcPersonController.saveHTCPerson(htcPerson);
                            HTCPerson createdHTCPerson = htcPersonController.getHTCPerson(htcPerson.getUuid());
                            MuzimaHtcForm muzimaHtcForm = createHTCFormInstance(createdHTCPerson);
                            htcFormController.saveHTCForm(muzimaHtcForm);
                            MuzimaHtcForm createdHTCForm = htcFormController.getHTCForm(muzimaHtcForm.getUuid());
                            createdHTCForm.setHtcPerson(createdHTCPerson);
                            Log.e(getClass().getSimpleName(), "UUID e ID : " + createdHTCForm.getUuid() + " - " + createdHTCForm.getId());
                            searchResults.add(new PatientItem(createdHTCPerson));

                            ViewUtil.displayAlertDialog(HTCFormActivity.this,getResources().getString(R.string.record_saved_sucessfull)).show();
                            goToMainActivity(searchResults);
                        }
                    }
                    catch (MuzimaHTCFormController.MuzimaHTCFormSaveException e) {
                        ViewUtil.displayAlertDialog(HTCFormActivity.this,getResources().getString(R.string.htc_save_error)).show();
                    }
                }
            }
        });
    }

    private void initViews() {
        identificationDataBtn = findViewById(R.id.btn_identification_data);
        bookDataSectionBtn = findViewById(R.id.btn_book_data);
        atsTestingDataSectionBtn = findViewById(R.id.btn_ats_testing_data);
        historicalTestingSectionBtn = findViewById(R.id.btn_historical_testing_data);
        additionalInformationSectionBtn = findViewById(R.id.btn_additional_information_data);

        identificationDataLyt = findViewById(R.id.identification_data_lyt);
        bookDataSectionLyt = findViewById(R.id.book_info_data_lyt);
        atsTestingDataSectionLyt = findViewById(R.id.ats_testing_data_lyt);
        historicalTestingSectionLyt = findViewById(R.id.historical_testing_data_lyt);
        additionalInformationLyt = findViewById(R.id.additional_information_lyt);

        genderImg = findViewById(R.id.genderImg);
        name = findViewById(R.id.htcFullName);
        identifier = findViewById(R.id.htcNID);
        age = findViewById(R.id.htcAge);
        bookNumber = findViewById(R.id.bookNumber);
        bookPageNumber = findViewById(R.id.bookPageNumber);
        bookPageLine = findViewById(R.id.bookPageLine);
        testingSectors = findViewById(R.id.testingSectors);
        testingDate = findViewById(R.id.testingDate);
        popKeysMiners = findViewById(R.id.popKeysMiners);
        indexCaseContacts = findViewById(R.id.indexCaseContacts);
        testResult = findViewById(R.id.hivPositiveResult);
        selfTestConfirmation = findViewById(R.id.selfTestConfirmation);
        firstTimeTestedOption = findViewById(R.id.firstTimeTestedOption);
        pastPositiveOption = findViewById(R.id.pastPositiiveOption);
        dateOfCreation = findViewById(R.id.dateOfCreation);
        healthFacility = findViewById(R.id.healthFacility);
        saveHtcForm = findViewById(R.id.saveHtcForm);
        setPopKeyMinersOptions();
        setIndexCaseContactsOptions();
        setTestingSectorsOptions();
        setAdditionInformation();
    }
    private void setAdditionInformation() {
        try {
            String currentDate = DateUtils.getCurrentDateAsString();
            dateOfCreation.setText(currentDate);
            dateOfCreation.setEnabled(false);
            testingDate.setText(currentDate);

            Location location = getLocation();
            healthFacility.setText(location.getName());
            healthFacility.setEnabled(false);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean validateFields() {
        if (StringUtils.isEmpty(testingDate.getText().toString())) {
            ViewUtil.displayAlertDialog(HTCFormActivity.this,getResources().getString(R.string.testing_date_format_error)).show();
            return false;
        } else {
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                dateOfTesting = simpleDateFormat.parse(testingDate.getText().toString());
            } catch (ParseException e) {
                ViewUtil.displayAlertDialog(HTCFormActivity.this,getResources().getString(R.string.testing_date_format_error)).show();
                return false;
            }
        }
        return true;
    }

    private void setHtcPersonIdentificationData(Patient htcPerson) {
        String gender = htcPerson.getGender();
        if(!StringUtils.isEmpty(gender)) {
            genderImg.setImageResource(gender.equalsIgnoreCase("M") ? R.drawable.gender_male : R.drawable.gender_female);
        }
        name.setText(htcPerson.getDisplayName());
        if(htcPerson.getBirthdate()!=null) {
            int htcPersonAge = DateUtils.calculateAge(htcPerson.getBirthdate());
            age.setText(htcPersonAge+" Anos");
        }
        identifier.setText(!StringUtils.isEmpty(htcPerson.getIdentifier()) ? htcPerson.getIdentifier() : getResources().getString(R.string.htc_person_no_identifier));
    }

    private MuzimaHtcForm createHTCFormInstance(HTCPerson htcPerson) {
        MuzimaHtcForm muzimaHtcForm = null;
        if(!isEditionFlow) {
            muzimaHtcForm = new MuzimaHtcForm();
        } else {
            muzimaHtcForm = htcForm;
        }
        muzimaHtcForm.setHtcPerson(htcPerson);
        if(!StringUtils.isEmpty(bookNumber.getText().toString())) {
            Integer book = Integer.parseInt(bookNumber.getText().toString());
            muzimaHtcForm.setBookNumber(book);
        }
        if(!StringUtils.isEmpty(bookPageNumber.getText().toString())) {
            Integer pageNumber = Integer.parseInt(bookPageNumber.getText().toString());
            muzimaHtcForm.setBookPageNumber(pageNumber);
        }
        if(!StringUtils.isEmpty(bookPageLine.getText().toString())) {
            Integer pageLineNumber = Integer.parseInt(bookPageLine.getText().toString());
            muzimaHtcForm.setBookPageLineNumber(pageLineNumber);
        }
        if(testingSectors.getSelectedItem()!=null) {
            String sector = testingSectors.getSelectedItem().toString();
            muzimaHtcForm.setTestingSector(!StringUtils.isEmpty(sector)?sector:null);
        }
        if(popKeysMiners.getSelectedItem()!=null) {
            String popKeysMinersValue = popKeysMiners.getSelectedItem().toString();
            muzimaHtcForm.setPopKeysMiners(!StringUtils.isEmpty(popKeysMinersValue)?popKeysMinersValue:null);
        }
        if(indexCaseContacts.getSelectedItem()!=null) {
            String indexCaseContact = indexCaseContacts.getSelectedItem().toString();
            muzimaHtcForm.setIndexCaseContact(!StringUtils.isEmpty(indexCaseContact)?indexCaseContact:null);
        }
        if(!StringUtils.isEmpty(testingDate.getText().toString())) {
            try {
                Date date = DateUtils.parse(testingDate.getText().toString());
                muzimaHtcForm.setTestingDate(date);
            } catch (ParseException e) {
                ViewUtil.displayAlertDialog(HTCFormActivity.this,getResources().getString(R.string.testing_date_format_error)).show();
            }
        }
        if(firstTimeTestedOption.isChecked()) {
            muzimaHtcForm.setTestHistory("FIRST_TEST");
        }
        if(pastPositiveOption.isChecked()) {
            muzimaHtcForm.setTestHistory("PAST_POSITIVE");
        }
        muzimaHtcForm.setSelfTestConfirmation(selfTestConfirmation.isChecked());
        muzimaHtcForm.setHtcRetesting(false); // update
        if(!StringUtils.isEmpty(testResult.getText().toString())) {
            String results = testResult.getText().toString();
            muzimaHtcForm.setTestResult(results);
        }
        if(dateOfTesting!=null) {
            muzimaHtcForm.setTestingDate(dateOfTesting);
        }
        muzimaHtcForm.setMigrationState("PENDING");
        muzimaHtcForm.setGeneratedEncounterId(null);
        Location location = getLocation();
        muzimaHtcForm.setTestingLocation(location);
        return muzimaHtcForm;
    }
    private void setPopKeyMinersOptions() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.popKeysMiners,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        popKeysMiners.setAdapter(adapter);
    }
    private void setIndexCaseContactsOptions() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.indexCaseContacts,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        indexCaseContacts.setAdapter(adapter);
    }
    private void setTestingSectorsOptions() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.testingSectors,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        testingSectors.setAdapter(adapter);
    }
    private String setHivResult(Patient htcPerson) {
        int htcPersonAge = DateUtils.calculateAge(htcPerson.getBirthdate());
        String gender = htcPerson.getGender();
        String faixaEtaria = htcPersonAge>49?"[50+]":
                ((htcPersonAge<50 && htcPersonAge>24)?"[25-49]":((htcPersonAge<25 && htcPersonAge>19)?"[20-25]":
                        ((htcPersonAge<20 && htcPersonAge>14)?"[15-19]":(htcPersonAge<15 && htcPersonAge>9)?"[10-14]":(htcPersonAge<10 && htcPersonAge>1)?"[1-9]":"[<1]")));
        String hivResult = "Positivo - "+gender+" - "+faixaEtaria;
        return hivResult;
    }
    private void initController() {
        this.htcPersonController = ((MuzimaApplication) getApplicationContext()).getHtcPersonController();
        this.htcFormController = ((MuzimaApplication) getApplicationContext()).getHtcFormController();
    }

    private void goToMainActivity(List<PatientItem> searchResults) {
        Intent intent;
        intent = new Intent(getApplicationContext(), HTCMainActivity.class);
        intent.putExtra("searchResults", (Serializable) searchResults);
        startActivity(intent);
        finish();
    }
    private Location getLocation() {
        MuzimaSettingController muzimaSettingController = ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController();
        LocationController locationController = ((MuzimaApplication) getApplicationContext()).getLocationController();
        try {
            MuzimaSetting encounterLocationIdSetting = muzimaSettingController.getSettingByProperty(DEFAULT_ENCOUNTER_LOCATION_SETTING);
            if(encounterLocationIdSetting != null) {
                if(encounterLocationIdSetting.getValueString() != null) {
                    Location defaultEncounterLocation = locationController.getLocationById(Integer.valueOf(encounterLocationIdSetting.getValueString()));
                    return defaultEncounterLocation;
                }
            }
        } catch (
                MuzimaSettingController.MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Encountered an error while fetching setting ",e);
        } catch (
                LocationController.LocationLoadException e) {
            Log.e(getClass().getSimpleName(), "Encountered an error while fetching location ",e);
        }
        return null;
    }

    private void setHTCFormData() {
        if(htcForm!=null) {
            bookNumber.setText(htcForm.getBookNumber()+"");
            bookPageNumber.setText(htcForm.getBookPageNumber()+"");
            bookPageLine.setText(htcForm.getBookPageLineNumber()+"");
            testingDate.setText(DateUtils.convertDateToDayMonthYearString(htcForm.getTestingDate()));

            if (StringUtils.stringHasValue(htcForm.getTestingSector())) {
                int countTestingSectors = testingSectors.getAdapter().getCount();
                for (int i = 0; i < countTestingSectors; i++) {
                    if (testingSectors.getAdapter().getItem(i).toString().equalsIgnoreCase(htcForm.getTestingSector())) {
                        testingSectors.setSelection(i);
                        break;
                    }
                }
            }

            if (StringUtils.stringHasValue(htcForm.getPopKeysMiners())) {
                int countPopKeyMiners = popKeysMiners.getAdapter().getCount();
                for (int i = 0; i < countPopKeyMiners; i++) {
                    if (popKeysMiners.getAdapter().getItem(i).toString().equalsIgnoreCase(htcForm.getPopKeysMiners())) {
                        popKeysMiners.setSelection(i);
                        break;
                    }
                }
            }

            if (StringUtils.stringHasValue(htcForm.getIndexCaseContact())) {
                int countIndexCaseContacts = indexCaseContacts.getAdapter().getCount();
                for (int i = 0; i < countIndexCaseContacts; i++) {
                    if (indexCaseContacts.getAdapter().getItem(i).toString().equalsIgnoreCase(htcForm.getIndexCaseContact())) {
                        indexCaseContacts.setSelection(i);
                        break;
                    }
                }
            }

            if (StringUtils.stringHasValue(htcForm.getTestHistory())) {
                if (htcForm.getTestHistory().equalsIgnoreCase("FIRST_TEST")) {
                    firstTimeTestedOption.setChecked(true);
                } else if (htcForm.getTestHistory().equalsIgnoreCase("PAST_POSITIVE")) {
                    pastPositiveOption.setChecked(true);
                }
            }
            selfTestConfirmation.setChecked(htcForm.isSelfTestConfirmation());
            healthFacility.setText(htcForm.getTestingLocation().getName());
            if(htcPerson.getSyncStatus().equalsIgnoreCase(Constants.STATUS_UPLOADED)){
                enableOrDisableFields(false);
            }
        }
    }

    private void enableOrDisableFields(boolean enable) {
        bookNumber.setEnabled(enable);
        bookPageNumber.setEnabled(enable);
        bookPageLine.setEnabled(enable);
        testingDate.setEnabled(enable);
        testingSectors.setEnabled(enable);
        popKeysMiners.setEnabled(enable);
        indexCaseContacts.setEnabled(enable);
        firstTimeTestedOption.setEnabled(enable);
        pastPositiveOption.setEnabled(enable);
        selfTestConfirmation.setEnabled(enable);
        healthFacility.setEnabled(enable);
        dateOfCreation.setEnabled(enable);
        saveHtcForm.setVisibility(enable?View.VISIBLE:View.INVISIBLE);
    }
}
