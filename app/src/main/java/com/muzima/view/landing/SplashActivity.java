package com.muzima.view.landing;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.muzima.R;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BaseFragmentActivity;
import com.muzima.view.MainActivity;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.login.LoginActivity;

public class SplashActivity extends BaseFragmentActivity {

    private final ThemeUtils themeUtils = new ThemeUtils();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        themeUtils.onCreate(this);
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
//                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                startActivity(intent);
//                finish();
            }
        };

        countDownTimer.start();
    }
}
