package com.muzima.utils.smartcard;

import android.util.Log;

import com.muzima.api.model.SmartCardRecord;

public class SmartCardIntentResult {
    private SmartCardRecord smartCardRecord;
    private String errors;

    public void setSHRModel(SmartCardRecord smartCardRecord) {
        this.smartCardRecord = smartCardRecord;
    }

    public SmartCardRecord getSmartCardRecord() {
        return smartCardRecord;
    }

    public void setErrors(String errors) {
        this.errors = errors;
    }

    public String getErrors() {
        return errors;
    }

    public boolean isSuccessResult(){
        return smartCardRecord != null;
    }
}
