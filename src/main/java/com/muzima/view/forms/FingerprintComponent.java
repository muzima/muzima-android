package com.muzima.view.forms;

import android.app.Activity;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import com.muzima.utils.fingerprint.futronic.FingerprintResult;

import static com.muzima.utils.fingerprint.futronic.FingerPrintActivity.FINGERPRINT_DATA;

public class FingerprintComponent {

    private final Activity activity;
    public static String fieldName;
    public static final int REQUEST_CODE = 0x0000c0dc;

    public FingerprintComponent(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void startFingerprintIntent(String fieldName) {
        this.fieldName = fieldName;
        Intent fingerPrintIntent = new Intent(activity.getApplication(), com.muzima.utils.fingerprint.futronic.FingerPrintActivity.class);
        fingerPrintIntent.putExtra("action", 0);
        activity.startActivityForResult(fingerPrintIntent, REQUEST_CODE);
    }

    public static String getFieldName() {
        return fieldName;
    }

    public static FingerprintResult parseActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String fingerprintString = intent.getStringExtra(FINGERPRINT_DATA);
                return new FingerprintResult(fingerprintString, getFieldName());
            }
        }
        return null;
    }
}
