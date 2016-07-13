/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.util.Log;
import android.webkit.JavascriptInterface;
import com.muzima.api.model.Form;
import com.muzima.api.model.FormTemplate;

public class FormInstance {
    private static final String TAG = "FormInstance";
    private final Form form;
    private final FormTemplate formTemplate;

    public FormInstance(Form form, FormTemplate formTemplate) {
        this.form = form;
        this.formTemplate = formTemplate;
    }

    @JavascriptInterface
    public String getFormName(){
        return form.getName();
    }

    @JavascriptInterface
    public String getModel() {
        return formTemplate.getModel();
    }

    @JavascriptInterface
    public String getModelJson() {
        return formTemplate.getModelJson();
    }

    @JavascriptInterface
    public String getHTML(){
        return formTemplate.getHtml();
    }

    public void log(String message) {
        Log.d(TAG, message);
    }


}
