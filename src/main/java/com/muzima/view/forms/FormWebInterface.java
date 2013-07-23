package com.muzima.view.forms;

import android.util.Log;
//import android.webkit.JavascriptInterface;

import com.muzima.api.model.Form;
import com.muzima.api.model.FormTemplate;

public class FormWebInterface {
    private static final String TAG = "FormWebInterface";
    private final Form form;
    private final FormTemplate formTemplate;

    public FormWebInterface(Form form, FormTemplate formTemplate) {
        this.form = form;
        this.formTemplate = formTemplate;
    }

//    @JavascriptInterface
    public String getModel() {
        return formTemplate.getModel();
    }

//    @JavascriptInterface
    public String getFormName(){
        return form.getName();
    }

//    @JavascriptInterface
    public String getHtml(){
        return formTemplate.getHtml();
    }

//    public void goBack() {
//        activity.setResult(FORM_SUCCESSFULLY_SUBMITTED_RESULT_CODE);
//        activity.finish();
//    }

    public void log(String message) {
        Log.d(TAG, message);
    }
}
