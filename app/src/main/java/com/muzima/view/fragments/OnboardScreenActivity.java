package com.muzima.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.muzima.R;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.view.TermsAndPolicyActivity;
import com.muzima.view.login.LoginActivity;

public class OnboardScreenActivity extends AppCompatActivity {

    private View nextButton;
    private View skipButton;
    private ImageView firstDotView;
    private ImageView secondDotView;
    private ImageView thirdDotView;
    private TextView descriptionTextView;
    private int page;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_onboarding_screen);

        nextButton = findViewById(R.id.on_boarding_next_button);
        skipButton = findViewById(R.id.on_boarding_skip_button);
        firstDotView = findViewById(R.id.first_page_dot_view);
        secondDotView = findViewById(R.id.second_page_dot_view);
        thirdDotView = findViewById(R.id.third_page_dot_view);
        descriptionTextView = findViewById(R.id.on_boarding_description_text_view);

        firstDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_blue_dot));
        secondDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
        thirdDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
        descriptionTextView.setText("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.");

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (page) {
                    case 0:
                        page = 1;
                        firstDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
                        secondDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_blue_dot));
                        thirdDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
                        descriptionTextView.setText("Consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.");
                        break;
                    case 1:
                        skipButton.setVisibility(View.GONE);
                        page = 2;
                        firstDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
                        secondDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
                        thirdDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_blue_dot));
                        descriptionTextView.setText("Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.");
                        break;
                    case 2:
                        MuzimaPreferences.setOnBoardingCompletedPreference(getApplicationContext(), true);
                        Intent intent = new Intent(OnboardScreenActivity.this, TermsAndPolicyActivity.class);
                        startActivity(intent);
                        finish();
                        break;

                }
            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MuzimaPreferences.setOnBoardingCompletedPreference(getApplicationContext(), true);
                Intent intent = new Intent(OnboardScreenActivity.this, TermsAndPolicyActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
