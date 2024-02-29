package com.muzima.view.htc;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.HTCPerson;
import com.muzima.api.model.Location;
import com.muzima.api.model.MuzimaHtcForm;
import com.muzima.api.model.Patient;
import com.muzima.controller.HTCPersonController;
import com.muzima.controller.MuzimaHTCFormController;
import com.muzima.model.patient.PatientItem;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.utils.ViewUtil;
import com.muzima.view.main.HTCMainActivity;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    RadioButton pastPositiiveOption;
    EditText dateOfCreation;
    EditText healthFacility;
    ImageView saveHtcForm;
    private Date dateOfTesting;
    private Patient htcPerson;
    private boolean isNewPerson;
    private List<PatientItem> searchResults;
    private HTCPersonController htcPersonController;
    private MuzimaHTCFormController htcFormController;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_htcform);
        searchResults = (List<PatientItem>) getIntent().getSerializableExtra("searchResults");
        isNewPerson = (Boolean) getIntent().getSerializableExtra("isNewPerson");
        if(isNewPerson) {
            htcPerson = (Patient) getIntent().getSerializableExtra("newHTCPerson");
        } else {
            htcPerson = (Patient) getIntent().getSerializableExtra("selectedPerson");
        }
        initViews();
        initController();
        setHtcPersonIdentificationData(htcPerson);
        testResult.setText(setHivResult(htcPerson));
        testResult.setEnabled(false);
        setListners();
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
            DatePickerFragment newFragment = new DatePickerFragment();
            newFragment.show(getFragmentManager(), "datePicker");
        });

        saveHtcForm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(validateFields()) {
                    try {
                        htcPersonController.saveHTCPerson((HTCPerson) htcPerson);
                        HTCPerson createdHTCPerson = htcPersonController.getHTCPerson(htcPerson.getUuid());
                        MuzimaHtcForm muzimaHtcForm = createHTCFormInstance(createdHTCPerson);
                        /*htcFormController.saveHTCForm(muzimaHtcForm);
                        MuzimaHtcForm createdHTCForm = htcFormController.getHTCForm(muzimaHtcForm.getUuid());
                        createdHTCForm.setHtcPerson(createdHTCPerson);
                        Log.e(getClass().getSimpleName(), "UUID e ID : " + createdHTCForm.getUuid() + " - " + createdHTCForm.getId());*/
                        searchResults.add(new PatientItem(createdHTCPerson));
                        AlertDialog.Builder builder = new AlertDialog.Builder(HTCFormActivity.this);
                        builder.setCancelable(false)
                                .setIcon(ThemeUtils.getIconWarning(getApplicationContext()))
                                .setTitle(getResources().getString(R.string.general_success))
                                .setMessage(getResources().getString(R.string.record_saved_sucessfull))
                                .setPositiveButton(R.string.general_ok, launchDashboard())
                                .show();
                        goToMainActivity(searchResults);
                    }
                    // MuzimaHTCFormController.MuzimaHTCFormSaveException
                    catch (Exception e) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(HTCFormActivity.this);
                        builder.setCancelable(false)
                                .setIcon(ThemeUtils.getIconWarning(getApplicationContext()))
                                .setTitle(getResources().getString(R.string.general_error))
                                .setMessage(getResources().getString(R.string.testing_date_format_error))
                                .setPositiveButton(R.string.general_ok, launchDashboard())
                                .show();
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
        pastPositiiveOption = findViewById(R.id.pastPositiiveOption);
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

            healthFacility.setText("Unidade Sanitaria");
            healthFacility.setEnabled(false);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker.
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this.getContext(), this, year, month, day);
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

            // Create a new instance of DatePickerDialog and return it.
            return datePickerDialog;
        }

        @Override
        public void onDateSet(DatePicker datePicker, int year, int month, int day) {
            StringBuilder monthValue = new StringBuilder();
            if(month+"".length()==1){
                monthValue.append("0");
                monthValue.append(month);
            } else {
                monthValue.append(month);
            }
            testingDate.setText(day + "-" + monthValue.toString() + "-" + year);
        }
    }

    private boolean validateFields() {
        if (StringUtils.isEmpty(testingDate.getText().toString())) {
            AlertDialog.Builder builder = new AlertDialog.Builder(HTCFormActivity.this);
            builder.setCancelable(false)
                    .setIcon(ThemeUtils.getIconWarning(getApplicationContext()))
                    .setTitle(getResources().getString(R.string.general_error))
                    .setMessage(getResources().getString(R.string.testing_date_format_error))
                    .setPositiveButton(R.string.general_ok, launchDashboard())
                    .show();
            return false;
        } else {
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                dateOfTesting = simpleDateFormat.parse(testingDate.getText().toString());
            } catch (ParseException e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(HTCFormActivity.this);
                builder.setCancelable(false)
                        .setIcon(ThemeUtils.getIconWarning(getApplicationContext()))
                        .setTitle(getResources().getString(R.string.general_error))
                        .setMessage(getResources().getString(R.string.testing_date_format_error))
                        .setPositiveButton(R.string.general_ok, launchDashboard())
                        .show();
                return false;
            }
        }
        return true;
    }

    private DialogInterface.OnClickListener launchDashboard() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //startActivity(new Intent(getApplicationContext(), HTCFormActivity.class));
                //finish();
            }
        };
    }

    private void parseTestingDate(String testingDate) {

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            dateOfTesting = simpleDateFormat.parse(testingDate);
        } catch (ParseException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(HTCFormActivity.this);
            builder.setCancelable(false)
                    .setIcon(ThemeUtils.getIconWarning(getApplicationContext()))
                    .setTitle(getResources().getString(R.string.general_error))
                    .setMessage(getResources().getString(R.string.testing_date_format_error))
                    .setPositiveButton(R.string.general_ok, launchDashboard())
                    .show();
        }

    }

    private void setHtcPersonIdentificationData(Patient htcPerson) {
        String gender = htcPerson.getGender();
        if(!StringUtils.isEmpty(gender)) {
            genderImg.setImageResource(gender.equalsIgnoreCase("M") ? R.drawable.gender_male : R.drawable.gender_female);
        }
        name.setText(htcPerson.getDisplayName());
        if(htcPerson.getBirthdate()!=null) {
            int htcPersonAge = DateUtils.calculateAge(htcPerson.getBirthdate());
            age.setText(htcPersonAge+"");
        }
        identifier.setText(htcPerson.getIdentifier()); //validar para pessoa que vem do SESP
    }

    private MuzimaHtcForm createHTCFormInstance(HTCPerson htcPerson) {
        MuzimaHtcForm muzimaHtcForm = new MuzimaHtcForm();
        muzimaHtcForm.setHtcPerson(htcPerson);
        if(!StringUtils.isEmpty(bookNumber.getText().toString())) {
            int book = Integer.parseInt(bookNumber.getText().toString());
            muzimaHtcForm.setBookNumber(book);
        }
        if(!StringUtils.isEmpty(bookPageNumber.getText().toString())) {
            int pageNumber = Integer.parseInt(bookPageNumber.getText().toString());
            muzimaHtcForm.setBookPageNumber(pageNumber);
        }
        if(!StringUtils.isEmpty(bookPageLine.getText().toString())) {
            int pageLineNumber = Integer.parseInt(bookPageLine.getText().toString());
            muzimaHtcForm.setBookPageLineNumber(pageLineNumber);
        }
        if(testingSectors.getSelectedItem()!=null) {
            String sector = testingSectors.getSelectedItem().toString();
            muzimaHtcForm.setTestingSector(sector);
        }
        if(popKeysMiners.getSelectedItem()!=null) {
            String popKeysMinersValue = popKeysMiners.getSelectedItem().toString();
            muzimaHtcForm.setPopKeysMiners(popKeysMinersValue);
        }
        if(indexCaseContacts.getSelectedItem()!=null) {
            String indexCaseContact = indexCaseContacts.getSelectedItem().toString();
            muzimaHtcForm.setIndexCaseContact(indexCaseContact);
        }
        if(!StringUtils.isEmpty(testResult.getText().toString())) {
            String results = testResult.getText().toString();
            muzimaHtcForm.setTestResult(results);
        }
        if(!StringUtils.isEmpty(testingDate.getText().toString())) {
            try {
                Date date = DateUtils.parse(testingDate.getText().toString());
                muzimaHtcForm.setTestingDate(date);
            } catch (ParseException e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(HTCFormActivity.this);
                builder.setCancelable(false)
                        .setIcon(ThemeUtils.getIconWarning(getApplicationContext()))
                        .setTitle(getResources().getString(R.string.general_error))
                        .setMessage(getResources().getString(R.string.testing_date_format_error))
                        .setPositiveButton(R.string.general_ok, launchDashboard())
                        .show();
            }
        }
        if(firstTimeTestedOption.isChecked()) {
            muzimaHtcForm.setTestHistory("FIRST_TEST");
        }
        if(pastPositiiveOption.isChecked()) {
            muzimaHtcForm.setTestHistory("PAST_POSITIVE");
        }
        muzimaHtcForm.setOthers("");
        muzimaHtcForm.setSelfTestConfirmation(selfTestConfirmation.isChecked());
        muzimaHtcForm.setHtcRetesting(false); // update
        if(!StringUtils.isEmpty(testResult.getText().toString())) {
            String results = testResult.getText().toString();
            muzimaHtcForm.setTestResult(results);
        }
        if(!StringUtils.isEmpty(testResult.getText().toString())) {
            String results = testResult.getText().toString();
            muzimaHtcForm.setTestResult(results);
        }
        if(dateOfTesting!=null) {
            muzimaHtcForm.setTestingDate(dateOfTesting);
        }
        muzimaHtcForm.setMigrationState("PENDING");
        muzimaHtcForm.setGeneratedEncounterId(null);
        muzimaHtcForm.setTestingLocation(null);
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
        String faixaEtaria = htcPersonAge>49?"50+":
                ((htcPersonAge<50 && htcPersonAge>24)?"25-49":((htcPersonAge<25 && htcPersonAge>19)?"20-25":
                        ((htcPersonAge<20 && htcPersonAge>14)?"15-19":(htcPersonAge<15 && htcPersonAge>9)?"10-14":(htcPersonAge<10 && htcPersonAge>1)?"1-9":"<1")));
        String hivResult = "Positivo - "+gender+" - "+faixaEtaria;
        return hivResult;
    }
    private void initController() {
        this.htcPersonController = ((MuzimaApplication) getApplicationContext()).getHtcPersonController();
        this.htcFormController = ((MuzimaApplication) getApplicationContext()).getHtcFormController();
    }

    private void goToMainActivity(List<PatientItem> searchResults) {
        Intent intent = new Intent(getApplicationContext(), HTCMainActivity.class);
        intent.putExtra("searchResults", (Serializable) searchResults);
        startActivity(intent);
        finish();
    }
}