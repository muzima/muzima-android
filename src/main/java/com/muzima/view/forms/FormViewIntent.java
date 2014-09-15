/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.model.BaseForm;
import com.muzima.model.FormWithData;

public class FormViewIntent extends Intent {
    public FormViewIntent(Activity activity, FormWithData form) {
        this(activity, form, form.getPatient());
    }

    public FormViewIntent(Activity activity, BaseForm form, Patient patient) {
        super(activity, getClassBasedOnFormType(activity, form));
        putExtra(HTMLFormWebViewActivity.FORM, form);
        putExtra(HTMLFormWebViewActivity.PATIENT, patient);
    }

    private static Class getClassBasedOnFormType(Activity activity, BaseForm form) {
        FormController formController = ((MuzimaApplication) activity.getApplication()).getFormController();
        try {
            if (formController.getFormTemplateByUuid(form.getFormUuid()).isHTMLForm()) {
                return HTMLFormWebViewActivity.class;
            }
        } catch (FormController.FormFetchException e) {
            Log.e("FormIntent", "Error while identifying form to load it in WebView", e);
        }
        return FormWebViewActivity.class;
    }
}
