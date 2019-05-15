package com.muzima.messaging.twofactoraunthentication;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.RateLimitException;
import org.whispersystems.signalservice.internal.push.LockedException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.messaging.animations.AnimationCompleteListener;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.crypto.UnidentifiedAccessUtil;
import com.muzima.messaging.customcomponents.VerificationCodeView;
import com.muzima.messaging.customcomponents.VerificationPinKeyboard;
import com.muzima.messaging.push.AccountManagerFactory;
import com.muzima.messaging.utils.Util;
import com.muzima.utils.Permissions;
import com.muzima.utils.concurrent.AssertedSuccessListener;
import com.muzima.view.SplashActivity;
import com.muzima.view.login.LoginActivity;

public class SmsCodeVerificationActivity extends AppCompatActivity implements VerificationCodeView.OnCodeEnteredListener{
    private static final int SCENE_TRANSITION_DURATION = 250;
    TextView title;
    TextView subtitle;
    View registrationContainer;
    View verificationContainer;
    View pinContainer;
    View pinClarificationContainer;
    VerificationPinKeyboard keyboard;
    VerificationCodeView verificationCodeView;
    Button pinButton;
    TextView pinForgotButton;
    EditText pin;

    int callCountdown = 64;
    String e164number = "error-invalid-164number";
    String password = "error-invalid-password";
    FloatingActionButton fab;
    SignalServiceAccountManager accountManager;

    RegistrationState registrationState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_sms_verification_code);

        this.title = findViewById(R.id.verify_header);
        this.subtitle = findViewById(R.id.verify_subheader);
        this.fab = findViewById(R.id.fab);
        this.registrationContainer = findViewById(R.id.registration_container);
        this.verificationContainer = findViewById(R.id.verification_container);
        this.pinClarificationContainer = findViewById(R.id.pin_clarification_container);
        this.keyboard = findViewById(R.id.keyboard);
        this.verificationCodeView = findViewById(R.id.code);
        this.pinContainer = findViewById(R.id.pin_container);
        this.pinButton = findViewById(R.id.pinButton);
        this.pinForgotButton = findViewById(R.id.forgot_button);
        this.pin = findViewById(R.id.pin);

        String gcmToken = TextSecurePreferences.getGcmRegistrationId(SmsCodeVerificationActivity.this);

        if(gcmToken == null){
            Toast.makeText(getApplicationContext(),"Unable to perform registration at this time",Toast.LENGTH_SHORT).show();
            startActivity( new Intent(SmsCodeVerificationActivity.this, SplashActivity.class));
        }
        Log.e(getClass().getSimpleName(), "gcmToken value " + gcmToken);
        Optional<String> gcmTokenOptional = Optional.of(gcmToken);

        Bundle extras = getIntent().getExtras();
        e164number = extras.getString("e164number");
        password = extras.getString("password");

        Log.w(SmsCodeVerificationActivity.class.getSimpleName(),"push server password " + password);


        this.registrationState = new RegistrationState(RegistrationState.State.INITIAL, e164number, password, gcmTokenOptional);
        accountManager = AccountManagerFactory.createManager(SmsCodeVerificationActivity.this, e164number, password);

        animateTitle();

        animateSubTitle();

        animateFab();

        animateRegistrationVerificationContainer();


        registrationContainer.setVisibility(View.INVISIBLE);
        verificationContainer.setVisibility(View.VISIBLE);

        this.keyboard.setOnKeyPressListener(key -> {
            if (key >= 0) verificationCodeView.append(key);
            else verificationCodeView.delete();
        });

        this.verificationCodeView.setOnCompleteListener(this);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void animateRegistrationVerificationContainer() {
        registrationContainer.animate().translationX(-1 * registrationContainer.getWidth()).setDuration(SCENE_TRANSITION_DURATION).setListener(new AnimationCompleteListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                registrationContainer.clearAnimation();
                registrationContainer.setVisibility(View.INVISIBLE);
                registrationContainer.setTranslationX(0);

                verificationContainer.setTranslationX(verificationContainer.getWidth());
                verificationContainer.setVisibility(View.VISIBLE);
                verificationContainer.animate().translationX(0).setListener(null).setInterpolator(new OvershootInterpolator()).setDuration(SCENE_TRANSITION_DURATION).start();
            }
        }).start();
    }

    private void animateFab() {
        fab.animate().rotationBy(-360f).setDuration(SCENE_TRANSITION_DURATION).setListener(new AnimationCompleteListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                fab.clearAnimation();
                fab.setImageResource(R.drawable.ic_textsms_24dp);
                fab.animate().rotationBy(-375f).setDuration(SCENE_TRANSITION_DURATION).setListener(null).start();
            }
        }).start();
    }


    private void animateSubTitle() {
        subtitle.animate().translationX(-1 * subtitle.getWidth()).setDuration(SCENE_TRANSITION_DURATION).setListener(new AnimationCompleteListener() {

            @Override
            public void onAnimationEnd(Animator animation) {
                SpannableString subtitleDescription = new SpannableString(getString(R.string.RegistrationActivity_please_enter_the_verification_code_sent_to_s, e164number));
                SpannableString wrongNumber = new SpannableString(getString(R.string.registrationActivity_wrong_number));

                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                    }

                    @Override
                    public void updateDrawState(TextPaint paint) {
                        paint.setColor(Color.WHITE);
                        paint.setUnderlineText(true);
                    }
                };

                wrongNumber.setSpan(clickableSpan, 0, wrongNumber.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                subtitle.setText(new SpannableStringBuilder(subtitleDescription).append(" ").append(wrongNumber));
                subtitle.setMovementMethod(LinkMovementMethod.getInstance());
                subtitle.clearAnimation();
                subtitle.setTranslationX(subtitle.getWidth());
                subtitle.animate().translationX(0).setListener(null).setInterpolator(new OvershootInterpolator()).setDuration(SCENE_TRANSITION_DURATION).start();
            }
        }).start();
    }

    private void animateTitle() {
        title.animate().translationX(-1 * title.getWidth()).setDuration(SCENE_TRANSITION_DURATION).setListener(new AnimationCompleteListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                title.setText(getString(R.string.registrationActivity_verify_s, e164number));
                title.clearAnimation();
                title.setTranslationX(title.getWidth());
                title.animate().translationX(0).setListener(null).setInterpolator(new OvershootInterpolator()).setDuration(SCENE_TRANSITION_DURATION).start();
            }
        }).start();
    }

    private void verifyAccount(@NonNull String code, @Nullable String pin) throws IOException {
        int registrationId = KeyHelper.generateRegistrationId(false);
        byte[] unidentifiedAccessKey = UnidentifiedAccessUtil.getSelfUnidentifiedAccessKey(SmsCodeVerificationActivity.this);
        boolean universalUnidentifiedAccess = TextSecurePreferences.isUniversalUnidentifiedAccess(SmsCodeVerificationActivity.this);

        TextSecurePreferences.setLocalRegistrationId(SmsCodeVerificationActivity.this, registrationId);
        //SessionUtil.archiveAllSessions(SmsCodeVerificationActivity.this);

        String signalingKey = Util.getSecret(52);

        accountManager.verifyAccountWithCode(code, signalingKey, registrationId, !registrationState.getGcmToken().isPresent(), pin,
                unidentifiedAccessKey, universalUnidentifiedAccess);

        //IdentityKeyPair identityKey = IdentityKeyUtil.getIdentityKeyPair(SmsCodeVerificationActivity.this);
        //List<PreKeyRecord> records = PreKeyUtil.generatePreKeys(SmsCodeVerificationActivity.this);
       // SignedPreKeyRecord signedPreKey = PreKeyUtil.generateSignedPreKey(SmsCodeVerificationActivity.this, identityKey, true);

       // accountManager.setPreKeys(identityKey.getPublicKey(),signedPreKey,records);

        if (registrationState.getGcmToken().isPresent()) {
            accountManager.setGcmId(registrationState.getGcmToken());
        }

        Log.w(SmsCodeVerificationActivity.class.getSimpleName(),"Gcm Registration Token | " + registrationState.getGcmToken().orNull());

        TextSecurePreferences.setGcmRegistrationId(SmsCodeVerificationActivity.this, registrationState.getGcmToken().orNull());
        TextSecurePreferences.setGcmDisabled(SmsCodeVerificationActivity.this, !registrationState.getGcmToken().isPresent());
        TextSecurePreferences.setWebsocketRegistered(SmsCodeVerificationActivity.this, true);

//        DatabaseFactory.getIdentityDatabase(SmsCodeVerificationActivity.this)
//                .saveIdentity(SignalAddress.fromSerialized(registrationState.getE164number()),
//                        identityKey.getPublicKey(), IdentityDatabase.VerifiedStatus.VERIFIED,
//                        true, System.currentTimeMillis(), true);

        TextSecurePreferences.setVerifying(SmsCodeVerificationActivity.this, false);
        TextSecurePreferences.setPushRegistered(SmsCodeVerificationActivity.this, true);
        TextSecurePreferences.setLocalNumber(SmsCodeVerificationActivity.this, registrationState.getE164number());
        TextSecurePreferences.setPushServerPassword(SmsCodeVerificationActivity.this, registrationState.getPassword());
        TextSecurePreferences.setSignalingKey(SmsCodeVerificationActivity.this, signalingKey);
        TextSecurePreferences.setSignedPreKeyRegistered(SmsCodeVerificationActivity.this, true);
        TextSecurePreferences.setPromptedPushRegistration(SmsCodeVerificationActivity.this, true);
        TextSecurePreferences.setUnauthorizedReceived(SmsCodeVerificationActivity.this, false);
    }

    private void handleSuccessfulRegistration() {

//        MuzimaApplication.getInstance(SmsCodeVerificationActivity.this).getJobManager().add(new DirectoryRefreshJob(SmsCodeVerificationActivity.this, false));
//        MuzimaApplication.getInstance(SmsCodeVerificationActivity.this).getJobManager().add(new RotateCertificateJob(SmsCodeVerificationActivity.this));


        /**
         * Save registration status in launch preference file
         * to orchestrate Future App Launches.
         */

        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_launch_preference_file), Context.MODE_PRIVATE);
        if (sharedPreferences != null) {
            sharedPreferences
                    .edit()
                    .putBoolean(getString(R.string.signal_user_verified_preference), true)
                    .apply();
        } else
            Toast.makeText(getApplicationContext(), "An error Occured while saving your registration.", Toast.LENGTH_LONG).show();


        Intent nextIntent = getIntent().getParcelableExtra("next_intent");

        if (nextIntent == null) {
            nextIntent = new Intent(SmsCodeVerificationActivity.this, LoginActivity.class);
        }
        startActivity(nextIntent);
        finish();
    }

    @SuppressLint("StaticFieldLeak")
    private void handlePhoneCallRequest() {
        if (registrationState.getState() == RegistrationState.State.VERIFYING) {
            //callMeCountDownView.startCountDown(300);

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        accountManager.requestVoiceVerificationCode();
                    } catch (IOException e) {
                        Log.w(getClass().getSimpleName(), e);
                    }

                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    @SuppressLint("StaticFieldLeak")
    @Override
    public void onCodeComplete(@NonNull String code) {
        this.registrationState = new RegistrationState(RegistrationState.State.CHECKING, this.registrationState);
        //callMeCountDownView.setVisibility(View.INVISIBLE);
        keyboard.displayProgress();

        new AsyncTask<Void, Void, Pair<Integer, Long>>() {
            @Override
            protected Pair<Integer, Long> doInBackground(Void... voids) {
                try {
                    verifyAccount(code, null);
                    return new Pair<>(1, -1L);
                } catch (LockedException e) {
                    Log.w(getClass().getSimpleName(), e);
                    return new Pair<>(2, e.getTimeRemaining());
                } catch (IOException e) {
                    Log.w(getClass().getSimpleName(), e);
                    return new Pair<>(3, -1L);
                }
            }

            @Override
            protected void onPostExecute(Pair<Integer, Long> result) {
                if (result.first == 1) {
                    keyboard.displaySuccess().addListener(new AssertedSuccessListener<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            handleSuccessfulRegistration();
                        }
                    });
                } else if (result.first == 2) {
                    keyboard.displayLocked().addListener(new AssertedSuccessListener<Boolean>() {
                        @Override
                        public void onSuccess(Boolean r) {
                            registrationState = new RegistrationState(RegistrationState.State.PIN, registrationState);
                            displayPinView(code, result.second);
                        }
                    });
                } else {
                    keyboard.displayFailure().addListener(new AssertedSuccessListener<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            registrationState = new RegistrationState(RegistrationState.State.VERIFYING, registrationState);
                          //  callMeCountDownView.setVisibility(View.VISIBLE);
                            verificationCodeView.clear();
                            keyboard.displayKeyboard();
                        }
                    });
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void displayPinView(String code, long lockedUntil) {
        title.animate().translationX(-1 * title.getWidth()).setDuration(SCENE_TRANSITION_DURATION).setListener(new AnimationCompleteListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                title.setText(R.string.RegistrationActivity_registration_lock_pin);
                title.clearAnimation();
                title.setTranslationX(title.getWidth());
                title.animate().translationX(0).setListener(null).setInterpolator(new OvershootInterpolator()).setDuration(SCENE_TRANSITION_DURATION).start();
            }
        }).start();

        subtitle.animate().translationX(-1 * subtitle.getWidth()).setDuration(SCENE_TRANSITION_DURATION).setListener(new AnimationCompleteListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                subtitle.setText(R.string.RegistrationActivity_this_phone_number_has_registration_lock_enabled_please_enter_the_registration_lock_pin);
                subtitle.clearAnimation();
                subtitle.setTranslationX(subtitle.getWidth());
                subtitle.animate().translationX(0).setListener(null).setInterpolator(new OvershootInterpolator()).setDuration(SCENE_TRANSITION_DURATION).start();
            }
        }).start();

        verificationContainer.animate().translationX(-1 * verificationContainer.getWidth()).setDuration(SCENE_TRANSITION_DURATION).setListener(new AnimationCompleteListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                verificationContainer.clearAnimation();
                verificationContainer.setVisibility(View.INVISIBLE);
                verificationContainer.setTranslationX(0);

                pinContainer.setTranslationX(pinContainer.getWidth());
                pinContainer.setVisibility(View.VISIBLE);
                pinContainer.animate().translationX(0).setListener(null).setInterpolator(new OvershootInterpolator()).setDuration(SCENE_TRANSITION_DURATION).start();
            }
        }).start();

        fab.animate().rotationBy(-360f).setDuration(SCENE_TRANSITION_DURATION).setListener(new AnimationCompleteListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                fab.clearAnimation();
               // fab.setImageResource(R.drawable.ic_lock_white_24dp);
                fab.animate().rotationBy(-360f).setDuration(SCENE_TRANSITION_DURATION).setListener(null).start();
            }
        }).start();

        pinButton.setOnClickListener(v -> handleVerifyWithPinClicked(code, pin.getText().toString()));
        pinForgotButton.setOnClickListener(v -> handleForgottenPin(lockedUntil));
        pin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && code.equals(s.toString()))
                    pinClarificationContainer.setVisibility(View.VISIBLE);
                else if (pinClarificationContainer.getVisibility() == View.VISIBLE)
                    pinClarificationContainer.setVisibility(View.GONE);
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    private void handleVerifyWithPinClicked(@NonNull String code, @Nullable String pin) {
        if (TextUtils.isEmpty(pin) || TextUtils.isEmpty(pin.replace(" ", ""))) {
            Toast.makeText(this, R.string.RegistrationActivity_you_must_enter_your_registration_lock_PIN, Toast.LENGTH_LONG).show();
            return;
        }

//        pinButton.setIndeterminateProgressMode(true);
//        pinButton.setProgress(50); TODO com.dd.CircleProgressButton bugs; fix to uncomment; turn pinButton to
//        todo: to com.dd.CircularProgressButton

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                try {
                    verifyAccount(code, pin);
                    return 1;
                } catch (LockedException e) {
                    Log.w(getClass().getSimpleName(), e);
                    return 2;
                } catch (RateLimitException e) {
                    Log.w(getClass().getSimpleName(), e);
                    return 3;
                } catch (IOException e) {
                    Log.w(getClass().getSimpleName(), e);
                    return 4;
                }
            }

            @Override
            protected void onPostExecute(Integer result) {
//                pinButton.setIndeterminateProgressMode(false);
//                pinButton.setProgress(0); todo fix com.dd.* library integration to use CircularProgressButton

                if (result == 1) {
                    TextSecurePreferences.setRegistrationLockPin(SmsCodeVerificationActivity.this, pin);
                    TextSecurePreferences.setRegistrationtLockEnabled(SmsCodeVerificationActivity.this, true);
                    TextSecurePreferences.setRegistrationLockLastReminderTime(SmsCodeVerificationActivity.this, System.currentTimeMillis());
                    //TextSecurePreferences.setRegistrationLockNextReminderInterval(SmsCodeVerificationActivity.this, RegistrationLockReminders.INITIAL_INTERVAL);

                    handleSuccessfulRegistration();
                } else if (result == 2) {
                    SmsCodeVerificationActivity.this.pin.setText("");
                    Toast.makeText(SmsCodeVerificationActivity.this, R.string.RegistrationActivity_incorrect_registration_lock_pin, Toast.LENGTH_LONG).show();
                } else if (result == 3) {
                    new AlertDialog.Builder(SmsCodeVerificationActivity.this)
                            .setTitle(R.string.RegistrationActivity_too_many_attempts)
                            .setMessage(R.string.RegistrationActivity_you_have_made_too_many_incorrect_registration_lock_pin_attempts_please_try_again_in_a_day)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                } else if (result == 4) {
                    Toast.makeText(SmsCodeVerificationActivity.this, R.string.RegistrationActivity_error_connecting_to_service, Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    private void handleForgottenPin(long timeRemaining) {
        new AlertDialog.Builder(SmsCodeVerificationActivity.this)
                .setTitle(R.string.registration_activity_oh_no)
                .setMessage(getString(R.string.RegistrationActivity_registration_of_this_phone_number_will_be_possible_without_your_registration_lock_pin_after_seven_days_have_passed, (TimeUnit.MILLISECONDS.toDays(timeRemaining) + 1)))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}


