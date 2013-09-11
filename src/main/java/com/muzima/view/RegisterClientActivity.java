package com.muzima.view;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

import com.muzima.R;

public class RegisterClientActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_client);
        // Show the Up button in the action bar.
        setupActionBar();


        //---RadioButton---
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radio_button_register_sex);
        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radio_button_register_male = (RadioButton) findViewById(R.id.radio_button_register_male);
                if (radio_button_register_male.isChecked()) {
                    Toast.makeText(getBaseContext(),
                            "Option 1 checked!",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getBaseContext(),
                            "Option 2 checked!",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {

        getActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.register_client, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
