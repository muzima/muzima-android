/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.model;

public class SetupActionLogModel{
    private String setupAction;
    private String setupActionResult;
    private String setupActionResultStatus;

    public String getSetupAction() {
        return setupAction;
    }

    public void setSetupAction(String setupAction) {
        this.setupAction = setupAction;
    }

    public String getSetupActionResult() {
        return setupActionResult;
    }

    public void setSetupActionResult(String setupActionResult) {
        this.setupActionResult = setupActionResult;
    }

    public void setSetupActionResultStatus(String setupActionResultStatus) {
        this.setupActionResultStatus = setupActionResultStatus;
    }

    public String getSetupActionResultStatus() {
        return setupActionResultStatus;
    }
}