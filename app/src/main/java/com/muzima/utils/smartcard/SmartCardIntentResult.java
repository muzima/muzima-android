package com.muzima.utils.smartcard;

import android.util.Log;

import com.muzima.api.model.SmartCardRecord;

import java.util.List;

public class SmartCardIntentResult {
    private SmartCardRecord smartCardRecord;
    private List<String> errors;

    public void setSHRModel(SmartCardRecord smartCardRecord) {
        this.smartCardRecord = smartCardRecord;
    }

    public SmartCardRecord getSmartCardRecord() {
        return smartCardRecord;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean isSuccessResult(){
        return smartCardRecord != null;
    }
}
