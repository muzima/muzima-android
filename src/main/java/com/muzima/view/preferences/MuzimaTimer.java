package com.muzima.view.preferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.search.api.util.StringUtil;
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
        Log.e("MuzimaTimer", "About to timeout ");
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
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(muzimaApplication);
        String passwordKey = muzimaApplication.getResources().getString(R.string.preference_password);
        settings.edit().putString(passwordKey, StringUtil.EMPTY).commit();

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