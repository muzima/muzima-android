package com.muzima.view.preferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.view.login.LoginActivity;

public class MuzimaTimer extends CountDownTimer {

    private static int DEFAULT_TIMEOUT_IN_MIN = 5;
    private MuzimaApplication muzimaApplication;
    private static MuzimaTimer muzimaTimer;

    private MuzimaTimer(long millisInFuture, long countDownInterval, MuzimaApplication muzimaApplication) {
        super(millisInFuture, countDownInterval);
        this.muzimaApplication = muzimaApplication;
        setDefaultTimeOut();
    }

    public static MuzimaTimer getTimer(MuzimaApplication muzimaApplication) {
        if (muzimaTimer == null) {
            muzimaTimer = new MuzimaTimer(getTimeInMillis(DEFAULT_TIMEOUT_IN_MIN) , getTimeInMillis(DEFAULT_TIMEOUT_IN_MIN), muzimaApplication);
        }
        return muzimaTimer;
    }

    public void setTimeOutInMillis(int timeOutInMin) {
        muzimaTimer.cancel();
        muzimaTimer = new MuzimaTimer(getTimeInMillis(timeOutInMin), getTimeInMillis(timeOutInMin), muzimaApplication);
        muzimaTimer.start();
    }

    @Override
    public void onTick(long l) {
    }


    @Override
    public void onFinish() {
        logOut();
    }

    public void restart() {
        this.cancel();
        this.start();
    }

    private void logOut() {
        muzimaApplication.logOut();
        launchLoginActivity();
    }

    private void launchLoginActivity() {
        Intent intent = new Intent(muzimaApplication, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(LoginActivity.isFirstLaunch, false);
        intent.putExtra(LoginActivity.sessionTimeOut, true);
        muzimaApplication.startActivity(intent);
    }

    private void setDefaultTimeOut() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(muzimaApplication);
        String passwordKey = muzimaApplication.getResources().getString(R.string.preference_timeout);
        settings.edit().putString(passwordKey, String.valueOf(DEFAULT_TIMEOUT_IN_MIN)).commit();
    }

    private static long getTimeInMillis(int timeOutInMin) {
        return timeOutInMin * 60 * 1000;
    }

}