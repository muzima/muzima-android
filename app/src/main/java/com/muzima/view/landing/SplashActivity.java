/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.landing;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.Nullable;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.view.BaseAuthenticatedActivity;
import com.muzima.view.initialwizard.SetupMethodPreferenceWizardActivity;

public class SplashActivity extends BaseAuthenticatedActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        handleInitializeTimer();
    }

    private void handleInitializeTimer() {
        long durationMillis = 3000;
        CountDownTimer countDownTimer = new CountDownTimer(durationMillis, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if(((MuzimaApplication) getApplicationContext()).getAuthenticatedUser() != null &&
                        !new WizardFinishPreferenceService(SplashActivity.this).isWizardFinished()){
                    Intent intent = new Intent(getApplicationContext(), SetupMethodPreferenceWizardActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

        countDownTimer.start();
    }
}
