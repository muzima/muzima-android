/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.utils.smartcard;

import com.muzima.api.model.SmartCardRecord;

import java.util.List;

public class SmartCardIntentResult {
    private SmartCardRecord smartCardRecord;
    private List<String> errors;

    public void setSmartCardRecord(SmartCardRecord smartCardRecord) {
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
