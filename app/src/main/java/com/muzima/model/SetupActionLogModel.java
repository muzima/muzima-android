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