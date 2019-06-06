package com.muzima.messaging.twofactoraunthentication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.muzima.view.BaseActivity;
import com.muzima.R;


public class CountrySelectionActivity extends BaseActivity implements CountrySelectionFragment.CountrySelectedListener {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.setContentView(R.layout.country_selection);
    }

    @Override
    public void countrySelected(String countryName, int countryCode) {
        Intent result = getIntent();
        result.putExtra("country_name", countryName);
        result.putExtra("country_code", countryCode);

        this.setResult(RESULT_OK, result);
        this.finish();
    }
}