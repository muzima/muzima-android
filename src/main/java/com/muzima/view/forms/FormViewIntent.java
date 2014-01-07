package com.muzima.view.forms;

import android.app.Activity;
import android.content.Intent;
import com.muzima.api.model.Patient;
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
        super(activity, FormWebViewActivity.class);
        putExtra(FormWebViewActivity.FORM, form);
        putExtra(FormWebViewActivity.PATIENT, patient);
        putExtra(FormWebViewActivity.DISCRIMINATOR, discriminator);
    }
}
