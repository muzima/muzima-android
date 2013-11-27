package com.muzima.view.preferences;

import android.content.Intent;
import android.os.CountDownTimer;
import com.muzima.MuzimaApplication;
import com.muzima.view.login.LoginActivity;

public class MuzimaTimer extends CountDownTimer {

    private static final long TIME_OUT_IN_MILLIS = 10000;
    private static final long TIME_OUT_COUNTDOWN = 1000;
    private MuzimaApplication muzimaApplication;
    private static MuzimaTimer muzimaTimer;

    private MuzimaTimer(long millisInFuture, long countDownInterval, MuzimaApplication muzimaApplication) {
        super(millisInFuture, countDownInterval);
        this.muzimaApplication = muzimaApplication;
    }

    public static MuzimaTimer getTimer(MuzimaApplication muzimaApplication) {
        if (muzimaTimer == null) {
            muzimaTimer = new MuzimaTimer(TIME_OUT_IN_MILLIS, TIME_OUT_COUNTDOWN, muzimaApplication);
        }
        return muzimaTimer;
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
}