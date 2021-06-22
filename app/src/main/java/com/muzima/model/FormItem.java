package com.muzima.model;

import com.muzima.api.model.Form;

public class FormItem {
    private Form form;
    private boolean selected;

    public FormItem(Form form, boolean selected) {
        this.form = form;
        this.selected = selected;
    }

    public Form getForm() {
        return form;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
