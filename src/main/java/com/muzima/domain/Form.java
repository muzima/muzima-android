package com.muzima.domain;

import java.util.List;

public interface Form {
    public String getId();
    public String getName();
    public String getDescription();
    public List<String> getTags();
}
