package com.muzima.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.muzima.R;
import com.muzima.utils.MuzimaPreferenceUtils;
import com.muzima.view.MainActivity;

public class OnboardingFirstScreenActivity extends AppCompatActivity {

    private Button nextButton;
    private Button skipButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_onboarding_first_screen);

        nextButton = findViewById(R.id.on_boarding_next_button);
        skipButton = findViewById(R.id.on_boarding_skip_button);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), OnboardingSecondScreenActivity.class);
                startActivity(intent);
                finish();
            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MuzimaPreferenceUtils.setOnBoardingCompletedPreference(getApplicationContext(), true);
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
