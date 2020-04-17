package com.muzima.utils.fhir;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;
import com.muzima.R;
import com.muzima.api.model.Patient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FhirIntentIntegrator {

    private static final String ACTION_READ_RESOURCE = "com.muzima.muzimafhir.ACTION_REQUEST_RESOURCE";
    private static final String ACTION_WRITE_RESOURCE = "com.muzima.muzimafhir.ACTION_WRITE_RESOURCE";
    private static final int RESOURCE_READ_REQUEST_CODE = 69;
    private static final int RESOURCE_WRITE_RESOURCE_CODE = 70;
    private static final List<String> RESOURCE_TYPES = new ArrayList<>(Arrays.asList("patient", "observation", "encounter"));
    private static Gson gson = new Gson();
    private final Activity activity;

    // delete this
    TextView tv;

    public FhirIntentIntegrator(Activity activity) {
        this.activity = activity;
        tv = activity.findViewById(R.id.infoTV); // delete this
    }

    public static boolean isReadRequest(int requestCode) {
        return requestCode == RESOURCE_READ_REQUEST_CODE;
    }

    public static boolean isWriteRequest(int requestCode) {
        return requestCode == RESOURCE_WRITE_RESOURCE_CODE;
    }

    public void initiateResourceRead(String resourceType, String queryType) {
        Intent intent = new Intent();
        intent.setAction(ACTION_READ_RESOURCE);
        intent.putExtra("resourceType", "patient");
        intent.putExtra("queryType", "getOne");
        intent.putExtra("id", "5e2eb69b21c7a2122726889f");
        intent.setType("text/plain");
        startIntentActivityForResult(intent, RESOURCE_READ_REQUEST_CODE);

    }

    private void startIntentActivityForResult(Intent intent, int requestCode){
        PackageManager packageManager = activity.getPackageManager();
        if(intent.resolveActivity(packageManager) != null) {
            activity.startActivityForResult(intent, requestCode);
        } else {
            showFhirDialog();
        }
    }

    private void showFhirDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
        dialog.setTitle("Fhir nto found");
        dialog.setMessage("Fhir app not installed");
        dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        dialog.setNegativeButton(R.string.general_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        dialog.show();
    }

    public static Patient parseActivityResult(int requestCode, int resultCode, Intent intent) throws Exception {
        if (requestCode == RESOURCE_READ_REQUEST_CODE || requestCode == RESOURCE_WRITE_RESOURCE_CODE) {
            if(intent == null){
                throw new Exception("Cannot get result intent");
            }

            Patient patient = new Patient();
            if (requestCode == RESOURCE_READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                String resourceData = intent.getStringExtra("resource");
                String resourceType = intent.getStringExtra("resourceType");
                String queryType = intent.getStringExtra("queryType");

                if (resourceType != null && queryType != null) {
                    patient = gson.fromJson(resourceData, Patient.class);
                }
            }
            return patient;

        }
        return null;
    }


}
