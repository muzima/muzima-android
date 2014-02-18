package com.muzima.view.forms;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.model.BaseForm;
import com.muzima.model.FormWithData;

import static com.muzima.utils.Constants.FORM_DISCRIMINATOR_ENCOUNTER;

public class FormViewIntent extends Intent {
    public FormViewIntent(Activity activity, FormWithData form) {
        this(activity, form, form.getPatient(), FORM_DISCRIMINATOR_ENCOUNTER);
    }

    public FormViewIntent(Activity activity, BaseForm form, Patient patient) {
        this(activity, form, patient, FORM_DISCRIMINATOR_ENCOUNTER);
    }

    public FormViewIntent(Activity activity, BaseForm form, Patient patient, String discriminator) {
        super(activity, getClassBasedOnFormType(activity, form));
        putExtra(HTMLFormWebViewActivity.FORM, form);
        putExtra(HTMLFormWebViewActivity.PATIENT, patient);
        putExtra(HTMLFormWebViewActivity.DISCRIMINATOR, discriminator);
    }

    private static Class getClassBasedOnFormType(Activity activity, BaseForm form) {
        FormController formController = ((MuzimaApplication) activity.getApplication()).getFormController();
        try {
            if (formController.getFormTemplateByUuid(form.getFormUuid()).isHTMLForm()) {
                return HTMLFormWebViewActivity.class;
            }
        } catch (FormController.FormFetchException e) {
            Log.e("FormIntent", "Error while identifying form to load it in WebView");
        }
        return FormWebViewActivity.class;
    }
}
