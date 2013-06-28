package com.muzima.domain;


import java.util.List;

public class Html5Form implements Form {
    private String id;
    private String name;
    private String description;
    private List<String> tags;

    public Html5Form(String id, String name, String description, List<String> tags){
        this.id = id;
        this.name = name;
        this.description = description;
        this.tags = tags;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<String> getTags() {
        return tags;
    }
}
