/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.util.Log;
import android.webkit.JavascriptInterface;
import com.muzima.api.model.Form;
import com.muzima.api.model.FormTemplate;

class FormInstance {
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
    public String getModelXml() {
        return formTemplate.getModelXml();
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
        Log.d(getClass().getSimpleName(), message);
    }


}
