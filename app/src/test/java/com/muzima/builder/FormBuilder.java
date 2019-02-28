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

import com.muzima.api.model.Form;
import com.muzima.api.model.Tag;

public class FormBuilder {
    private String name;
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

    public FormBuilder withTags(Tag [] tags) {
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
