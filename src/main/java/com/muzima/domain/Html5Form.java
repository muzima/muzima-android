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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Html5Form html5Form = (Html5Form) o;

        if (description != null ? !description.equals(html5Form.description) : html5Form.description != null)
            return false;
        if (id != null ? !id.equals(html5Form.id) : html5Form.id != null) return false;
        if (name != null ? !name.equals(html5Form.name) : html5Form.name != null) return false;
        if (tags != null ? !tags.equals(html5Form.tags) : html5Form.tags != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        return result;
    }
}
