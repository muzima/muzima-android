package com.muzima.builder;

import com.muzima.api.model.Form;
import com.muzima.api.model.Tag;

public class FormBuilder {
    public String name;
    private String uuid;
    private String description;
    private String version;
    private Tag[] tags;

    public static FormBuilder form() {
        return new FormBuilder();
    }

    public FormBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public FormBuilder withUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public FormBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public FormBuilder withVersion(String version) {
        this.version = version;
        return this;
    }

    public FormBuilder withTags(Tag[] tags) {
        this.tags = tags;
        return this;
    }

    public Form build(){
        Form form = new Form();
        form.setName(name);
        form.setUuid(uuid);
        form.setDescription(description);
        form.setVersion(version);
        form.setTags(tags);
        return form;
    }
}
