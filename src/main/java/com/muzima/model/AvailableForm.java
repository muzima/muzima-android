package com.muzima.model;

import com.muzima.api.model.Tag;

public class AvailableForm extends BaseForm {
    private Tag[] tags;
    private boolean isDownloaded;

    public Tag[] getTags() {
        return tags;
    }

    public void setTags(Tag[] tags) {
        this.tags = tags;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }
}
