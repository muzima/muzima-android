package com.muzima.view;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.api.model.Patient;
import com.muzima.utils.fhir.FhirIntentIntegrator;

public class FhirResourceActivity extends AppCompatActivity {

    private static final String ACTION_REQUEST_RESOURCE = "com.muzima.muzimafhir.ACTION_REQUEST_RESOURCE";
    private static final String ACTION_WRITE_RESOURCE = "com.muzima.muzimafhir.ACTION_WRITE_RESOURCE";
    private static final int RESOURCE_READ_REQUEST_CODE = 69;
    private static final int RESOURCE_WRITE_RESOURCE_CODE = 70;

    private Button mGetResourceBtn;
    private TextView mShowResourceTV;
    private Patient patient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fhir_resource);

        mGetResourceBtn = findViewById(R.id.getResourceBtn);
        mShowResourceTV = findViewById(R.id.resourceTV);

        mGetResourceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readResource();
            }
        });
    }

    private void readResource() {
        final FhirIntentIntegrator integrator = new FhirIntentIntegrator(this);
        integrator.initiateResourceRead("patient", "getOne");
    }

    public void processResourceReadResult(int requestCode, int resultCode, Intent dataIntent) {
        try {
            patient = FhirIntentIntegrator.parseActivityResult(requestCode, resultCode, dataIntent);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Could not get resource");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case RESOURCE_READ_REQUEST_CODE:
                processResourceReadResult(requestCode, resultCode, data);
                String patientString = patient.toString() + "\n" +
                        patient.getSummary() + "\n" +
                        patient.getDisplayName();
                mShowResourceTV.setText(patientString);
                break;
            default:
                mShowResourceTV.setText("requestCode: " + requestCode +
                        "\nResultCode: " + resultCode);
        }

    }
}
