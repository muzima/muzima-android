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

import com.muzima.api.model.Tag;

public class TagBuilder {
    private String name;
    private String uuid;

    public static TagBuilder tag() {
        return new TagBuilder();
    }

    public TagBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public TagBuilder withUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public Tag build(){
        Tag tag = new Tag();
        tag.setName(name);
        tag.setUuid(uuid);
        return tag;
    }
}