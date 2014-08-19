/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.view.custom.ScrollViewWithDetection;
import com.muzima.view.login.LoginActivity;

/**
 * TODO: Write brief description about the class here.
 */
public class DisclaimerActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disclaimer);

        String disclaimerText = getResources().getString(R.string.wizard_disclaimer_desc);
        final TextView disclaimerTextView = (TextView) findViewById(R.id.disclaimer_text_view);
        disclaimerTextView.setText(Html.fromHtml(disclaimerText));
        Linkify.addLinks(disclaimerTextView, Linkify.WEB_URLS);

        final Button nextButton = (Button) findViewById(R.id.next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                navigateToNextActivity();
            }
        });

        Button prevButton = (Button) findViewById(R.id.previous);
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                finish();
            }
        });

        final ScrollViewWithDetection scrollViewWithDetection = (ScrollViewWithDetection) findViewById(R.id.disclaimer_scroller);
        scrollViewWithDetection.setOnBottomReachedListener(new ScrollViewWithDetection.OnBottomReachedListener() {
            @Override
            public void onBottomReached() {
                nextButton.setVisibility(View.VISIBLE);
            }
        });

        scrollViewWithDetection.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressLint("NewApi")
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                int textViewHeight = disclaimerTextView.getHeight();
                int scrollViewHeight = scrollViewWithDetection.getHeight();
                int scrollViewPaddingTop = scrollViewWithDetection.getPaddingTop();
                int scrollViewPaddingBottom = scrollViewWithDetection.getPaddingBottom();
                boolean scrollable = scrollViewHeight < textViewHeight + scrollViewPaddingTop + scrollViewPaddingBottom;
                if (!scrollable) {
                    nextButton.setVisibility(View.VISIBLE);
                }

                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    scrollViewWithDetection.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    scrollViewWithDetection.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        ((MuzimaApplication) getApplication()).cancelTimer();
    }

    private void navigateToNextActivity() {
        // write the setting to mark the user have accepted the disclaimer
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String disclaimerKey = getResources().getString(R.string.preference_disclaimer);
        settings.edit().putBoolean(disclaimerKey, true).commit();
        // transition to the login activity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.isFirstLaunch, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}