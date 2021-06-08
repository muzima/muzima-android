package com.muzima.view.landing;

import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.muzima.R;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BaseFragmentActivity;

public class SplashActivity extends BaseFragmentActivity {

    private final ThemeUtils themeUtils = new ThemeUtils();
    private Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        toolbar = findViewById(R.id.splash_action_bar_holder);
        setSupportActionBar(toolbar);

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

            }
        };

        countDownTimer.start();
    }

//    private void handleLaunchNextActivity() {
//        if (MuzimaPreferences.getOnBoardingCompletedPreference(getApplicationContext())){
//            Intent intent = new Intent(getApplicationContext(), OnboardScreenActivity.class);
//            startActivity(intent);
//            finish();
//        }else {
//            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
//            startActivity(intent);
//            finish();
//        }
//    }
}
