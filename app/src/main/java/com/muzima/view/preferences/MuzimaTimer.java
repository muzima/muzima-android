/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.preferences;

import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.service.MuzimaLoggerService;
import com.muzima.service.TimeoutPreferenceService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.login.LoginActivity;
import com.muzima.view.main.HTCMainActivity;

public class MuzimaTimer extends CountDownTimer {

    private final MuzimaApplication muzimaApplication;
    private static MuzimaTimer muzimaTimer;

    private MuzimaTimer(long millisInFuture, long countDownInterval, MuzimaApplication muzimaApplication) {
        super(millisInFuture, countDownInterval);
        this.muzimaApplication = muzimaApplication;
    }

    public static MuzimaTimer getTimer(MuzimaApplication muzimaApplication) {
        if (muzimaTimer == null) {
            int timeout = new TimeoutPreferenceService(muzimaApplication).getTimeout();
            muzimaTimer = new MuzimaTimer(getTimeInMillis(timeout) , 5000, muzimaApplication);
        }
        return muzimaTimer;
    }


    public MuzimaTimer resetTimer(int timeOutInMin) {
        muzimaTimer.cancel();
        muzimaTimer = new MuzimaTimer(getTimeInMillis(timeOutInMin), 5000, muzimaApplication);
        muzimaTimer.start();
        return muzimaTimer;
    }

    @Override
    public void onTick(long l) {
        boolean isWizardComplete = new WizardFinishPreferenceService(muzimaApplication).isWizardFinished();
        try {
            if (muzimaApplication.getAuthenticatedUser() != null && isWizardComplete) {
                if (l * 0.001 <= 30) {
                    Intent intent;
                    Class mainClass = MainDashboardActivity.class;

                    if (muzimaApplication.getSetupConfigurationController().getAllSetupConfigurations().get(0).getUuid().equals("1eaa9574-fa5a-4655-bd63-466b538c5b5d")) {
                        mainClass = HTCMainActivity.class;
                        return;
                    }
                    intent = new Intent(muzimaApplication, mainClass);
                    intent.putExtra("AutoLogOutTimer", true);
                    intent.putExtra("RemainingTime", l);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    muzimaApplication.startActivity(intent);
                }
            }
        }catch (Exception e){
            Log.e(getClass().getSimpleName(),"Encountered Exception ",e);
        } catch (SetupConfigurationController.SetupConfigurationDownloadException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onFinish() {
        if(muzimaApplication.getAuthenticatedUser() != null) {
            logOut();
        }
    }

    public void restart() {
        this.cancel();
        this.start();
    }

    private void logOut()
    {
        MuzimaLoggerService.stopLogsSync();
        MuzimaLoggerService.log(muzimaApplication,"SESSION_TIMEOUT","{}");
        boolean isRunningInBackground = muzimaApplication.isRunningInBackground();
        boolean isWizardComplete = new WizardFinishPreferenceService(muzimaApplication).isWizardFinished();
        if (isWizardComplete) {
            muzimaApplication.logOut();
        }
        if(!isRunningInBackground && isWizardComplete) {
            launchLoginActivity();
        }
    }

    private void launchLoginActivity() {
        Intent intent = new Intent(muzimaApplication, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(LoginActivity.isFirstLaunch, false);
        intent.putExtra(LoginActivity.sessionTimeOut, true);
        muzimaApplication.startActivity(intent);
    }

    private static long getTimeInMillis(int timeOutInMin) {
        return timeOutInMin * 60 * 1000;
    }
}
