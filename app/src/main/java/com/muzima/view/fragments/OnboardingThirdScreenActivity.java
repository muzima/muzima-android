package com.muzima.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.muzima.R;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.view.login.LoginActivity;

public class OnboardingThirdScreenActivity extends AppCompatActivity {

    private Button nextButton;
    private Button skipButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_onboarding_third_screen);

        nextButton = findViewById(R.id.on_boarding_next_button);
        skipButton = findViewById(R.id.on_boarding_skip_button);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MuzimaPreferences.setOnBoardingCompletedPreference(getApplicationContext(), true);
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MuzimaPreferences.setOnBoardingCompletedPreference(getApplicationContext(), true);
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
