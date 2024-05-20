package com.muzima.view.person;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.HTCPerson;
import com.muzima.api.model.Location;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAddress;
import com.muzima.api.model.PersonName;
import com.muzima.controller.LocationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.model.patient.PatientItem;
import com.muzima.utils.DateUtils;
import com.muzima.utils.PhoneNumberUtils;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ViewUtil;
import com.muzima.view.BaseActivity;
import com.muzima.view.htc.HTCFormActivity;
import com.muzima.view.main.HTCMainActivity;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.muzima.util.Constants.ServerSettings.DEFAULT_ENCOUNTER_LOCATION_SETTING;

import com.muzima.utils.Constants;

public class PersonRegisterActivity extends BaseActivity {
    private ImageButton identificationDataBtn;
    private ImageButton birthDateSectionBtn;
    private ImageButton contactSectionBtn;
    private ImageButton sexSectionBtn;
    private ImageButton addressSectionBtn;
    private ImageButton additionalInformationSectionBtn;
    private LinearLayout identificationDataLyt;
    private LinearLayout birthDateSectionLyt;
    private LinearLayout contactSectionLyt;
    private LinearLayout sexSectionLyt;
    private LinearLayout addressSectionLyt;
    private LinearLayout additionalInformationLyt;
    private boolean identificationSection;
    private boolean birthDateSection;
    private boolean contactSection;
    private boolean sexSection;
    private boolean addressSection;
    private boolean additionalInformationSection;
    private ImageView savePerson;
    EditText name;
    EditText surname;
    RadioButton birthDateAge;
    RadioButton birthDateDate;
    static EditText birthDate;
    EditText htcPersonAge;
    TextView birthDateOrAgeTextView;
    RadioGroup sexType;
    RadioButton optMale;
    RadioButton optFemale;
    EditText contact;
    EditText street;
    EditText locality;
    EditText block;
    EditText neighbourhood;
    Spinner personExistsInSESP;
    TextView detailsTextView;
    EditText details;
    EditText dateOfCreation;
    EditText healthFacility;
    private List<PatientItem> searchResults;
    private Patient patient;
    private Boolean isAddATSForSESPExistingPerson;
    private Boolean isEditionFlow;
    Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_register);
        initViews();
        this.patient = (Patient) getIntent().getSerializableExtra("selectedPerson");
        isAddATSForSESPExistingPerson = (Boolean) getIntent().getSerializableExtra("isAddATSForSESPExistingPerson");
        isEditionFlow = (Boolean) getIntent().getSerializableExtra("isEditionFlow");
        this.searchResults = (List<PatientItem>) getIntent().getSerializableExtra("searchResults");
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.htc_person_register);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setDataFieldsForExistingSESPPersons();
        setListners();
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
            }
            else {
                identificationDataLyt.setVisibility(View.GONE);
                ViewUtil.expand(identificationDataLyt);
                identificationDataBtn.setImageResource(R.drawable.sharp_arrow_drop_up_24);
            }
        });
        birthDateSectionBtn.setOnClickListener(view -> {
            birthDateSection = !birthDateSection;
            if (birthDateSection) {
                birthDateSectionLyt.setVisibility(View.VISIBLE);
                ViewUtil.collapse(birthDateSectionLyt);
                birthDateSectionBtn.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            }
            else {
                birthDateSectionLyt.setVisibility(View.GONE);
                ViewUtil.expand(birthDateSectionLyt);
                birthDateSectionBtn.setImageResource(R.drawable.sharp_arrow_drop_up_24);
            }
        });
        contactSectionBtn.setOnClickListener(view -> {
            contactSection = !contactSection;
            if (contactSection) {
                contactSectionLyt.setVisibility(View.VISIBLE);
                ViewUtil.collapse(contactSectionLyt);
                contactSectionBtn.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            }
            else {
                contactSectionLyt.setVisibility(View.GONE);
                ViewUtil.expand(contactSectionLyt);
                contactSectionBtn.setImageResource(R.drawable.sharp_arrow_drop_up_24);
            }
        });
        sexSectionBtn.setOnClickListener(view -> {
            sexSection = !sexSection;
            if (sexSection) {
                sexSectionLyt.setVisibility(View.VISIBLE);
                ViewUtil.collapse(sexSectionLyt);
                sexSectionBtn.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            }
            else {
                sexSectionLyt.setVisibility(View.GONE);
                ViewUtil.expand(sexSectionLyt);
                sexSectionBtn.setImageResource(R.drawable.sharp_arrow_drop_up_24);
            }
        });
        addressSectionBtn.setOnClickListener(view -> {
            addressSection = !addressSection;
            if (addressSection) {
                addressSectionLyt.setVisibility(View.VISIBLE);
                ViewUtil.collapse(addressSectionLyt);
                addressSectionBtn.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            }
            else {
                addressSectionLyt.setVisibility(View.GONE);
                ViewUtil.expand(addressSectionLyt);
                addressSectionBtn.setImageResource(R.drawable.sharp_arrow_drop_up_24);
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
        personExistsInSESP.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedValue =  adapterView.getItemAtPosition(i).toString();
                if(selectedValue.equalsIgnoreCase("SIM")) {
                    hideDetailsFields(View.VISIBLE);
                } else {
                    hideDetailsFields(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        birthDateAge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                birthDateOrAgeTextView.setText("Idade *");
                htcPersonAge.setVisibility(View.VISIBLE);
                birthDate.setVisibility(View.GONE);
                htcPersonAge.setTop(birthDate.getTop());
                htcPersonAge.setHeight(birthDate.getHeight());
            }
        });
        birthDateDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                birthDateOrAgeTextView.setText("Data de Nascimento *");
                htcPersonAge.setVisibility(View.GONE);
                birthDate.setVisibility(View.VISIBLE);
            }
        });
        birthDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int mYear, mMonth, mDay;

                final Calendar c = Calendar.getInstance();
                mYear = c.get(Calendar.YEAR);
                mMonth = c.get(Calendar.MONTH);
                mDay = c.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog datePickerDialog = new DatePickerDialog(PersonRegisterActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                        birthDate.setText(DateUtils.getFormattedDate(DateUtils.createDate(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year, DateUtils.SIMPLE_DAY_MONTH_YEAR_DATE_FORMAT), DateUtils.SIMPLE_DAY_MONTH_YEAR_DATE_FORMAT));
                    }
                }, mYear, mMonth, mDay);
                datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
                datePickerDialog.show();
            }
        });
        savePerson.setOnClickListener(view -> {
                HTCPerson htcPerson = createHTCPersonInstance();
                if (htcPerson != null) {
                    goToHTCFormActivity(htcPerson);
                }
            });
        toolbar.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.drawable.ic_arrow_back:
                    Intent intent = new Intent(getApplicationContext(), HTCMainActivity.class);
                    startActivity(intent);
                    return true;
                default: ;
                    return false;
            }
        });
    }

    private void initViews() {
        identificationDataBtn = findViewById(R.id.btn_identification_data);
        birthDateSectionBtn = findViewById(R.id.btn_birth_data);
        contactSectionBtn = findViewById(R.id.btn_contact_data);
        sexSectionBtn = findViewById(R.id.btn_sex_data);
        addressSectionBtn = findViewById(R.id.btn_address_data);
        additionalInformationSectionBtn = findViewById(R.id.btn_additional_information_data);

        identificationDataLyt = findViewById(R.id.identification_data_lyt);
        birthDateSectionLyt = findViewById(R.id.birth_data_lyt);
        contactSectionLyt = findViewById(R.id.contact_data_lyt);
        sexSectionLyt = findViewById(R.id.sex_data_lyt);
        addressSectionLyt = findViewById(R.id.address_data_lyt);
        additionalInformationLyt = findViewById(R.id.additional_information_lyt);

        name = findViewById(R.id.name);
        surname = findViewById(R.id.surname);
        sexType = findViewById(R.id.sex_type);
        optMale = findViewById(R.id.male_rdb);
        optFemale = findViewById(R.id.female_rdb);
        birthDateAge = findViewById(R.id.birth_date_age);
        birthDateDate = findViewById(R.id.birth_date_date);
        birthDate = findViewById(R.id.birth_date);
        htcPersonAge = findViewById(R.id.htcPersonAge);
        birthDateOrAgeTextView = findViewById(R.id.birthDateOrAgeTextView);
        contact = findViewById(R.id.contact);
        street = findViewById(R.id.street);
        locality = findViewById(R.id.localidade);
        block = findViewById(R.id.quarteirao);
        neighbourhood = findViewById(R.id.bairro);
        personExistsInSESP = findViewById(R.id.personExistsInSESP);
        detailsTextView = findViewById(R.id.detailsTextView);
        details = findViewById(R.id.details);
        dateOfCreation = findViewById(R.id.dateOfCreation);
        healthFacility = findViewById(R.id.healthFacility);

        savePerson = findViewById(R.id.save_and_continue);

        setPersonExistsInSESPOptions();
        hideDetailsFields(View.VISIBLE);
        htcPersonAge.setVisibility(View.GONE);
        setAdditionInformation();
    }

    private void setDataFieldsForExistingSESPPersons() {
       if(this.patient!=null) {
           String existInSESP = ((HTCPerson) patient).getPersonExistInSESP();
           int countPopKeyMiners = personExistsInSESP.getAdapter().getCount();
           for(int i=0; i< countPopKeyMiners; i++) {
               if(personExistsInSESP.getAdapter().getItem(i).toString().equalsIgnoreCase(existInSESP)){
                   personExistsInSESP.setSelection(i);
                   break;
               }
           }
           if("Sim".equalsIgnoreCase(existInSESP)) {
               String existInSESPDetails = ((HTCPerson) patient).getPersonExistInSESPDetails();
               hideDetailsFields(View.VISIBLE);
               details.setText(existInSESPDetails);
           } else {
               hideDetailsFields(View.GONE);
               details.setText("");
           }
           name.setText(patient.getName().getGivenName());
           surname.setText(patient.getName().getFamilyName());
           if(StringUtils.stringHasValue(patient.getGender())) {
               if (patient.getGender().equalsIgnoreCase("M")) {
                   optMale.setChecked(true);
               } else {
                   optFemale.setChecked(true);
               }
           }
           if(patient.getBirthdate()!=null) {
               if(patient.getBirthdateEstimated()) {
                   birthDateAge.setChecked(true);
                   htcPersonAge.setVisibility(View.VISIBLE);
                   birthDateDate.setChecked(false);
                   birthDate.setVisibility(View.GONE);
                   birthDateOrAgeTextView.setText(getResources().getString(R.string.general_age));
                   int age = DateUtils.calculateAge(patient.getBirthdate());
                   htcPersonAge.setText(age+"");
               } else {
                   birthDateAge.setChecked(false);
                   htcPersonAge.setVisibility(View.GONE);
                   birthDateDate.setChecked(true);
                   birthDate.setVisibility(View.VISIBLE);
                   birthDateOrAgeTextView.setText(getResources().getString(R.string.general_birth_date));
                   birthDate.setText(DateUtils.convertDateToDayMonthYearString( patient.getBirthdate()).toString());
               }
           }
           if(!patient.getAddresses().isEmpty()) {
               street.setText(patient.getAddress().getAddress1());
               locality.setText(patient.getAddress().getAddress6());
               block.setText(patient.getAddress().getAddress3());
               neighbourhood.setText(patient.getAddress().getAddress5());
           }
           detailsTextView.setVisibility(View.VISIBLE);
           details.setVisibility(View.VISIBLE);
           String phoneNumber = ((HTCPerson) patient).getPhoneNumber();
           if(!StringUtils.isEmpty(phoneNumber)) {
               contact.setText(phoneNumber);
           }
           if(StringUtils.stringHasValue(((HTCPerson) patient).getSyncStatus()) && ((HTCPerson) patient).getSyncStatus().equalsIgnoreCase(Constants.STATUS_UPLOADED)){
               enableOrDisableFields(false);
           }
       }
    }

    private void hideDetailsFields(int visibility) {
        detailsTextView.setVisibility(visibility);
        details.setVisibility(visibility);
    }
    private void setAdditionInformation() {
        try {
            dateOfCreation.setText( DateUtils.getCurrentDateAsString());
            dateOfCreation.setEnabled(false);
            healthFacility.setText(getLocation().getName());
            healthFacility.setEnabled(false);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private HTCPerson createHTCPersonInstance() {
        if(!areRequiredFieldsValid()) {
               return null;
        }
        HTCPerson htcPerson = null;
        if(this.patient!=null) {

            if (isEditionFlow && isAddATSForSESPExistingPerson) {
                htcPerson = (HTCPerson) this.patient;
                htcPerson.setSespUuid(this.patient.getUuid());
                htcPerson.setPatient(Boolean.TRUE);
                htcPerson.setIdentifiers(this.patient.getIdentifiers());
                htcPerson.setAddresses(this.patient.getAddresses());
                htcPerson.setAttributes(this.patient.getAtributes());
            } else if (isEditionFlow && !isAddATSForSESPExistingPerson) {
                htcPerson = (HTCPerson) this.patient;
                htcPerson.setPatient(Boolean.FALSE);
                htcPerson.setIdentifiers(this.patient.getIdentifiers());
                htcPerson.setAddresses(this.patient.getAddresses());
                htcPerson.setAttributes(this.patient.getAtributes());
            } else if(!isEditionFlow && isAddATSForSESPExistingPerson) {
                htcPerson = new HTCPerson();
                htcPerson.setSespUuid(this.patient.getUuid());
                htcPerson.setPatient(Boolean.TRUE);
                htcPerson.setIdentifiers(this.patient.getIdentifiers());
                htcPerson.setAddresses(this.patient.getAddresses());
                htcPerson.setAttributes(this.patient.getAtributes());
            } else {
                htcPerson = new HTCPerson();
                htcPerson.setPatient(Boolean.FALSE);
            }
        }else {
            htcPerson = new HTCPerson();
            htcPerson.setPatient(Boolean.FALSE);
        }
        htcPerson.setGender(optMale.isChecked() ? "M" :(optFemale.isChecked() ? "F" : ""));
        htcPerson.setPhoneNumber(contact.getText().toString());
        if(birthDateDate.isChecked()) {
            try {
                Date birthDateValue = DateUtils.parse(birthDate.getText().toString());
                htcPerson.setBirthdate(birthDateValue);
                htcPerson.setBirthdateEstimated(false);
            } catch (ParseException e) {
                ViewUtil.displayAlertDialog(PersonRegisterActivity.this,"Por favor indicar uma data de nascimento válida." ).show();
                return null;
            }
        } else if(birthDateAge.isChecked()) {
          int age = Integer.parseInt(htcPersonAge.getText().toString());
          Date birthDateValue = null;
            try {
                birthDateValue = DateUtils.getEstimatedDate(age);
                htcPerson.setBirthdate(birthDateValue);
                htcPerson.setBirthdateEstimated(true);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        if (htcPerson.getNames() == null) htcPerson.setNames(new ArrayList<>());
        if (htcPerson.getNames().isEmpty()) {
            htcPerson.getNames().add(new PersonName());
            htcPerson.getNames().get(0).setPreferred(true);
        }

        PersonName personName = htcPerson.getName();
        personName.setGivenName(name.getText().toString());
        personName.setFamilyName(surname.getText().toString());
        personName.setPreferred(true);

        if (htcPerson.getAddress() == null) htcPerson.setAddresses(new ArrayList<>());
        if (htcPerson.getAddresses().isEmpty()) {
            htcPerson.getAddresses().add(new PersonAddress());
            htcPerson.getAddresses().get(0).setPreferred(true);
        }

        PersonAddress address = htcPerson.getAddress();
        address.setAddress6(locality.getText().toString());
        address.setAddress5(neighbourhood.getText().toString());
        address.setAddress3(block.getText().toString());
        address.setAddress1(street.getText().toString());
        address.setPreferred(true);

        String personExistsInSESPValue = personExistsInSESP.getSelectedItem().toString();
        htcPerson.setPersonExistInSESP(personExistsInSESPValue);

        if(details.getVisibility()== View.VISIBLE) {
            htcPerson.setPersonExistInSESPDetails(details.getText().toString());
        }
        return htcPerson;
    }

    private void goToHTCFormActivity(HTCPerson htcPerson) {
        Intent intent = new Intent(getApplicationContext(), HTCFormActivity.class);
        intent.putExtra("htcPerson", htcPerson);
        intent.putExtra("searchResults", (Serializable) searchResults);
        intent.putExtra("isAddATSForSESPExistingPerson", isAddATSForSESPExistingPerson);
        intent.putExtra("isEditionFlow", isEditionFlow);
        startActivity(intent);
        finish();
    }

    private void setPersonExistsInSESPOptions() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.personExistsInSESPOptions,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        personExistsInSESP.setAdapter(adapter);
    }

    private boolean areRequiredFieldsValid() {
        if(patient != null && StringUtils.stringHasValue(((HTCPerson) patient).getSyncStatus()) && ((HTCPerson) patient).getSyncStatus().equalsIgnoreCase(Constants.STATUS_UPLOADED)){
            return true;
        }

        String htcPersonName = name.getText().toString();
        if(StringUtils.isEmpty(htcPersonName) || StringUtils.hasNumbers(htcPersonName)) {
            ViewUtil.displayAlertDialog(PersonRegisterActivity.this,getResources().getString(R.string.htc_person_name_mandatory) ).show();
            return false;
        }
        String htcPersonSurname = surname.getText().toString();
        if(StringUtils.isEmpty(htcPersonSurname) || StringUtils.hasNumbers(htcPersonSurname)) {
            ViewUtil.displayAlertDialog(PersonRegisterActivity.this,getResources().getString(R.string.htc_person_surname_mandatory) ).show();
            return false;
        }
        if((!optMale.isChecked() && !optFemale.isChecked())) {
            ViewUtil.displayAlertDialog(PersonRegisterActivity.this,getResources().getString(R.string.htc_person_gender_mandatory) ).show();
            return false;
        }
        if(birthDateDate.isChecked() && StringUtils.isEmpty(birthDate.getText().toString())) {
            ViewUtil.displayAlertDialog(PersonRegisterActivity.this,"Por favor Indicar a data de Nascimento." ).show();
            return false;
        }
        if(birthDateAge.isChecked() && StringUtils.isEmpty(htcPersonAge.getText().toString())) {
            ViewUtil.displayAlertDialog(PersonRegisterActivity.this,"Por favor indicar idade." ).show();
            return false;
        }
        if(birthDateAge.isChecked()) {
            String age = htcPersonAge.getText().toString();
            if(!(age.length()>0 && age.length()<=3 && Integer.valueOf(age)>=0 && Integer.valueOf(age)<=100)) {
                ViewUtil.displayAlertDialog(PersonRegisterActivity.this,"Por favor indicar uma idade válida." ).show();
                return false;
            }
        }
        if(!StringUtils.isEmpty(contact.getText().toString())) {
            boolean isPhoneNumberValid = PhoneNumberUtils.validatePhoneNumber(contact.getText().toString());
            if(!isPhoneNumberValid) {
                ViewUtil.displayAlertDialog(PersonRegisterActivity.this,getResources().getString(R.string.htc_person_phone_number_error) ).show();
                return false;
            }
        }
        return true;
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

    private void enableOrDisableFields(boolean enable) {
        name.setEnabled(enable);
        surname.setEnabled(enable);
        optMale.setEnabled(enable);
        optFemale.setEnabled(enable);
        birthDateAge.setEnabled(enable);
        birthDateDate.setEnabled(enable);
        birthDate.setEnabled(enable);
        htcPersonAge.setEnabled(enable);
        contact.setEnabled(enable);
        street.setEnabled(enable);
        locality.setEnabled(enable);
        block.setEnabled(enable);
        neighbourhood.setEnabled(enable);
        personExistsInSESP.setEnabled(enable);
        details.setEnabled(enable);
        dateOfCreation.setEnabled(enable);
        healthFacility.setEnabled(enable);
    }
}
