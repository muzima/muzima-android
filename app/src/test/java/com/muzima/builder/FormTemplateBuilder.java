/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.builder;

import com.muzima.api.model.FormTemplate;

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
        formTemplate.setModelXml(model);
        formTemplate.setModelJson(modelJson);
        return formTemplate;
    }
}
