package com.muzima.builder;

import com.muzima.api.model.Form;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Tag;

public class FormTemplateBuilder {
    private String uuid;
    private String html;
    private String model;
    private String modelJson;

    public static FormTemplateBuilder formTemplate() {
        return new FormTemplateBuilder();
    }

    public FormTemplateBuilder withUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public FormTemplateBuilder withHtml(String html) {
        this.html = html;
        return this;
    }

    public FormTemplateBuilder withModel(String model) {
        this.model = model;
        return this;
    }

    public FormTemplateBuilder withModelJson(String modelJson) {
        this.modelJson = modelJson;
        return this;
    }

    public FormTemplate build(){
        FormTemplate formTemplate = new FormTemplate();
        formTemplate.setUuid(uuid);
        formTemplate.setHtml(html);
        formTemplate.setModel(model);
        formTemplate.setModelJson(modelJson);
        return formTemplate;
    }
}
