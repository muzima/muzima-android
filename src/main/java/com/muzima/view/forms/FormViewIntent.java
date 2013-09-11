package com.muzima.view.forms;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import com.muzima.model.BaseForm;
import com.muzima.model.FormWithData;

public class FormViewIntent extends Intent {
    public FormViewIntent(FragmentActivity activity, FormWithData form) {
        super(activity, FormWebViewActivity.class);
        putExtra(FormWebViewActivity.FORM_UUID, form.getFormUuid());
        putExtra(FormWebViewActivity.FORM_DATA_UUID, form.getFormDataUuid());
    }

    public FormViewIntent(FragmentActivity activity, BaseForm form, String patientId) {
        super(activity, FormWebViewActivity.class);
        putExtra(FormWebViewActivity.FORM_UUID, form.getFormUuid());
        putExtra(FormWebViewActivity.PATIENT_UUID, patientId);
    }
}
