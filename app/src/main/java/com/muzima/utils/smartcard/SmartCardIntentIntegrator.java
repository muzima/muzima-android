package com.muzima.utils.smartcard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import com.muzima.api.model.SmartCardRecord;
import com.muzima.utils.StringUtils;

import java.io.IOException;

public class SmartCardIntentIntegrator {
    private static String TAG = "CardIntentIntegrator";
    public static String ACTION_READ_DATA = "org.kenyahmis.psmart.ACTION_READ_DATA";

    public static String ACTION_WRITE_DATA = "org.kenyahmis.psmart.ACTION_WRITE_DATA";
    public static String EXTRA_AUTH_TOKEN = "AUTH_TOKEN";
    public static String AUTH_TOKEN_VALUE = "123";
    public static String EXTRA_ERRORS = "errors";
    public static String EXTRA_MESSAGE = "message";
    public static int SMARTCARD_READ_REQUEST_CODE = 98;
    public static int SMARTCARD_WRITE_REQUEST_CODE = 99;
    Activity activity;
    public SmartCardIntentIntegrator(Activity activity){
        this.activity = activity;
    }

    public static boolean isReadRequest(int requestCode){
        return requestCode == SMARTCARD_READ_REQUEST_CODE;
    }

    public static boolean isWriteRequest(int requestCode){
        return requestCode == SMARTCARD_WRITE_REQUEST_CODE;
    }

    public void initiateCardRead(){
        Intent intent = new Intent();
        intent.setAction(ACTION_READ_DATA);
        intent.setType("text/plain");
        intent.putExtra(EXTRA_AUTH_TOKEN,AUTH_TOKEN_VALUE);
        startIntentActivityForResult(intent,SMARTCARD_READ_REQUEST_CODE);

    }
    public void initiateCardWrite(String shrModel) throws IOException {
        Intent intent = new Intent();
        intent.setAction(ACTION_WRITE_DATA);
        intent.putExtra(EXTRA_AUTH_TOKEN,AUTH_TOKEN_VALUE);
        intent.setType("text/plain");
        intent.putExtra("ACTION","WRITE");
        intent.putExtra("WRITE_DATA",shrModel);
        startIntentActivityForResult(intent,SMARTCARD_WRITE_REQUEST_CODE);
    }
    public static SmartCardIntentResult parseActivityResult(int requestCode, int resultCode, Intent intent) throws Exception{
        if (requestCode == SMARTCARD_READ_REQUEST_CODE || requestCode == SMARTCARD_WRITE_REQUEST_CODE) {
            if(intent == null){
                throw new Exception("Cannot get result intent");
            }

            SmartCardIntentResult result = new SmartCardIntentResult();
            SmartCardRecord smartCardRecord = new SmartCardRecord();
            if (requestCode == SMARTCARD_READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                String jsonSHRModel = intent.getStringExtra(EXTRA_MESSAGE);
                smartCardRecord.setPlainPayload(jsonSHRModel);
                result.setSHRModel(smartCardRecord);
            }else if (requestCode == SMARTCARD_WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                String jsonSHRModel = intent.getStringExtra(EXTRA_MESSAGE);
                smartCardRecord.setEncryptedPayload(jsonSHRModel);
                result.setSHRModel(smartCardRecord);
            }else if (resultCode == Activity.RESULT_CANCELED) {
                String errors = intent.getStringExtra(EXTRA_ERRORS);
                if(StringUtils.isEmpty(errors)){
                    result.setErrors("Could not complete request");
                } else {
                    result.setErrors(errors);
                }
            }
            return result;
        }
        return null;
    }

    private void startIntentActivityForResult(Intent intent, int requestCode){
        PackageManager packageManager = activity.getPackageManager();
        if(intent.resolveActivity(packageManager) != null) {
            activity.startActivityForResult(intent, requestCode);
        } else {
            showInstallPsmartAppDownloadDialog();
        }
    }

    private AlertDialog showInstallPsmartAppDownloadDialog() {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(activity);
        downloadDialog.setTitle("Install P-Smart app");
        downloadDialog.setMessage("Would you like to install P-Smart app?");
        downloadDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String packageName = "org.kenyahmis.psmart";
                Uri uri = Uri.parse("market://details?id=" + packageName);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    activity.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.w(TAG, "Google Play is not installed; cannot install " + packageName, e);
                }
            }
        });
        downloadDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return downloadDialog.show();
    }
}
