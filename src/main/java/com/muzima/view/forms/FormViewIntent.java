package com.muzima.view.forms;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import com.muzima.api.model.Patient;
import com.muzima.model.BaseForm;
import com.muzima.model.FormWithData;

public class FormViewIntent extends Intent {
    public FormViewIntent(FragmentActivity activity, FormWithData form) {
        this(activity, form, form.getPatient());
    }

    public FormViewIntent(FragmentActivity activity, BaseForm form, Patient patient) {
        super(activity, FormWebViewActivity.class);
        putExtra(FormWebViewActivity.FORM, form);
        putExtra(FormWebViewActivity.PATIENT, patient);
    }
}
