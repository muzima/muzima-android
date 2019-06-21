package com.muzima.messaging.twofactoraunthentication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.i18n.phonenumbers.AsYouTypeFormatter;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.util.PhoneNumberFormatter;

import java.io.IOException;

import com.muzima.adapters.messaging.CountrySpinnerAdapter;
import com.muzima.messaging.push.AccountManagerFactory;
import com.muzima.utils.Permissions;
import com.muzima.view.login.LoginActivity;
import com.muzima.R;
import com.muzima.messaging.utils.Util;

public class RegistrationActivity extends AppCompatActivity {


    private static final int PICK_COUNTRY = 1;
    private static final int SCENE_TRANSITION_DURATION = 250;
    private static final int DEBUG_TAP_TARGET = 8;
    private static final int DEBUG_TAP_ANNOUNCE = 4;
    public static final  String CHALLENGE_EVENT = "com.muzima.CHALLENGE_EVENT";
    public static final  String CHALLENGE_EXTRA = "CAAChallenge";
    public static final  String RE_REGISTRATION_EXTRA = "re_registration";

    boolean isUserSmsVerified = false;
    SignalServiceAccountManager accountManager;
    Button registerButton;
    Spinner countrySelectSpinner;
    EditText countryCodeEditText;
    EditText phoneNumberEditText;
    String e164number = "no number - error";
    final String password = Util.getSecret(18);
    CountrySpinnerAdapter countrySpinnerAdapter;

    AsYouTypeFormatter countryFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number_verification);

        countryCodeEditText = findViewById(R.id.country_code);
        phoneNumberEditText = findViewById(R.id.number);
        countrySelectSpinner = findViewById(R.id.country_spinner);

        //check if user already passed 2factor auth from preferences
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.preference_sms_verification_file), Context.MODE_PRIVATE);
        if (sharedPreferences != null) {
            isUserSmsVerified = sharedPreferences.getBoolean(getString(R.string.preference_verify_sms_key), false);
            if (isUserSmsVerified){
                startActivity( new Intent(RegistrationActivity.this,LoginActivity.class));
            }
        }

        registerButton = findViewById(R.id.registerButton);
        countrySpinnerAdapter = new CountrySpinnerAdapter(getApplicationContext(), android.R.layout.simple_spinner_item);

        initializeSpinner();
        initializePermissions();
        initializeNumber();

    }

    @Override
    protected void onStart() {
        super.onStart();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                e164number = getConfiguredE164Number();

                if ((e164number != null && !e164number.isEmpty())) {
                    Toast.makeText(RegistrationActivity.this, getString(R.string.general_requesting_verification), Toast.LENGTH_LONG).show();
                    requestVerificationCode(password, e164number);
                } else
                    Toast.makeText(RegistrationActivity.this, getString(R.string.invalid_phone_number), Toast.LENGTH_LONG).show();

            }
        });
    }

    private void requestVerificationCode(String password, String e164number) {
        if (isUserSmsVerified) {
            Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            accountManager = AccountManagerFactory.createManager(RegistrationActivity.this, e164number, password);
            new SmsVerificationRequestBackgroundTask().execute();
            //request sms verification
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_COUNTRY && resultCode == RESULT_OK && data != null) {
            this.countryCodeEditText.setText(String.valueOf(data.getIntExtra("country_code", 1)));
            setCountryDisplay(data.getStringExtra("country_name"));
            setCountryFormatter(data.getIntExtra("country_code", 1));
        }
    }

    private void setCountryFormatter(int countryCode) {
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        String regionCode = util.getRegionCodeForCountryCode(countryCode);

        if (regionCode == null) this.countryFormatter = null;
        else this.countryFormatter = util.getAsYouTypeFormatter(regionCode);
    }

    private String getConfiguredE164Number() {
        String phoneNumber = phoneNumberEditText.getText().toString();
        String countryCode = countryCodeEditText.getText().toString();
        if (!phoneNumber.isEmpty() && !countryCode.isEmpty()) {
            return PhoneNumberFormatter.formatE164("+" + countryCode,
                    phoneNumber);
        } else {
            Toast.makeText(RegistrationActivity.this, getString(R.string.invalid_phone_number), Toast.LENGTH_LONG).show();
            return null;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private class SmsVerificationRequestBackgroundTask extends AsyncTask<Void, Void, Void> {

        private boolean isVerificationSmsSent = false;
        private String reasonForFailure = getString(R.string.general_unknown_error);

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                accountManager.requestSmsVerificationCode();
                isVerificationSmsSent = true;
            } catch (IOException e) {
                isVerificationSmsSent = false;
                if (e.getMessage().contains("Rate limit exceeded")) {
                    reasonForFailure = getString(R.string.sms_limit_exceeded);
                } else if (e.getMessage().contains("java.net.UnknownHostException: Unable to resolve host")) {
                    reasonForFailure = getString(R.string.server_unreachable);
                }
                Log.e(getClass().getSimpleName(), "Error Message " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!isVerificationSmsSent) {
                if (!reasonForFailure.isEmpty()) {
                    Toast.makeText(RegistrationActivity.this, reasonForFailure, Toast.LENGTH_LONG).show();
                } else
                    Toast.makeText(RegistrationActivity.this, getString(R.string.general_unable_to_send_sms_verification_error_message), Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent(RegistrationActivity.this, SmsCodeVerificationActivity.class);
                intent.putExtra("e164number", e164number);
                intent.putExtra("password",password);
                startActivity(intent);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initializeSpinner() {
        this.countrySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setCountryDisplay(getString(R.string.general_select_your_country));

        this.countrySelectSpinner.setAdapter(this.countrySpinnerAdapter);
        this.countrySelectSpinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Intent intent = new Intent(RegistrationActivity.this, CountrySelectionActivity.class);
                startActivityForResult(intent, PICK_COUNTRY);
            }
            return true;
        });
        this.countrySelectSpinner.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event.getAction() == KeyEvent.ACTION_UP) {
                Intent intent = new Intent(RegistrationActivity.this, CountrySelectionActivity.class);
                startActivityForResult(intent, PICK_COUNTRY);
                return true;
            }
            return false;
        });
    }

    @SuppressLint("MissingPermission")
    private void initializeNumber() {
        Optional<Phonenumber.PhoneNumber> localNumber = Optional.absent();

        if (Permissions.hasAll(this, Manifest.permission.READ_PHONE_STATE)) {
            localNumber = Util.getDeviceNumber(this);
        }

        if (localNumber.isPresent()) {
            this.countryCodeEditText.setText(String.valueOf(localNumber.get().getCountryCode()));
            this.phoneNumberEditText.setText(String.valueOf(localNumber.get().getNationalNumber()));
        } else {
            Optional<String> simCountryIso = Util.getSimCountryIso(this);

            if (simCountryIso.isPresent() && !TextUtils.isEmpty(simCountryIso.get())) {
                this.countryCodeEditText.setText(String.valueOf(PhoneNumberUtil.getInstance().getCountryCodeForRegion(simCountryIso.get())));
            }
        }
    }

    private void setCountryDisplay(String value) {
        this.countrySpinnerAdapter.clear();
        this.countrySpinnerAdapter.add(value);
    }

    @SuppressLint("InlinedApi")
    private void initializePermissions() {
        Permissions.with(RegistrationActivity.this)
                .request(Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.PROCESS_OUTGOING_CALLS)
                .ifNecessary()
                .withRationaleDialog(getString(R.string.general_contact_access),
                        R.drawable.ic_contacts_white_48dp, R.drawable.ic_folder_white_48dp)
                .onSomeGranted(permissions -> {
                    if (permissions.contains(Manifest.permission.READ_PHONE_STATE)) {
                        initializeNumber();
                    }
                })
                .execute();
    }
}


