package com.muzima.builder;

import com.muzima.api.model.Tag;

public class TagBuilder {
    public String name;
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
