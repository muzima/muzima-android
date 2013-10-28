package com.muzima.view.forms;

import android.app.Activity;
import android.content.Intent;
import com.muzima.api.model.Patient;
import com.muzima.model.BaseForm;
import com.muzima.model.FormWithData;

public class FormViewIntent extends Intent {
    public FormViewIntent(Activity activity, FormWithData form) {
        this(activity, form, form.getPatient());
    }

    public FormViewIntent(Activity activity, BaseForm form, Patient patient) {
        super(activity, FormWebViewActivity.class);
        putExtra(FormWebViewActivity.FORM, form);
        putExtra(FormWebViewActivity.PATIENT, patient);
    }
}
