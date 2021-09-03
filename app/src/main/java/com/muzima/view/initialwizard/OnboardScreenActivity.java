package com.muzima.view.initialwizard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.muzima.R;
import com.muzima.utils.MuzimaPreferences;

public class OnboardScreenActivity extends AppCompatActivity {

    private View nextButton;
    private View skipButton;
    private ImageView firstDotView;
    private ImageView secondDotView;
    private ImageView thirdDotView;
    private ImageView coverImageView;
    private TextView descriptionTextView;
    private TextView titleTextView;
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
        coverImageView = findViewById(R.id.on_boarding_cover_image_view);
        titleTextView = findViewById(R.id.on_boarding_title_text_view);

        firstDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_blue_dot));
        secondDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
        thirdDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
        coverImageView.setImageDrawable(getResources().getDrawable(R.drawable.openmrs_compatibility));
        titleTextView.setText(getResources().getString(R.string.general_openmrs_compatibility));
        descriptionTextView.setText(getResources().getString(R.string.general_openmrs_compatibility_description));

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (page) {
                    case 0:
                        page = 1;
                        coverImageView.setImageDrawable(getResources().getDrawable(R.drawable.multiple_use_cases));
                        firstDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
                        secondDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_blue_dot));
                        thirdDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
                        titleTextView.setText(getResources().getString(R.string.general_multiple_use_cases));
                        descriptionTextView.setText(getResources().getString(R.string.general_multiple_cases_description));
                        break;
                    case 1:
                        skipButton.setVisibility(View.GONE);
                        page = 2;
                        coverImageView.setImageDrawable(getResources().getDrawable(R.drawable.security_banner));
                        firstDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
                        secondDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
                        thirdDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_blue_dot));
                        titleTextView.setText( getResources().getString(R.string.general_security));
                        descriptionTextView.setText(getResources().getString(R.string.general_security_description));
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
