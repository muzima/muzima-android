package com.muzima.model;

import com.muzima.api.model.Tag;

public class AvailableForm extends BaseForm {
    public static final String REGISTRATION = "registration";
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

    public boolean isRegistrationForm(){
        if(tags == null){
            return false;
        }
        for (Tag tag : tags) {
            if(REGISTRATION.equalsIgnoreCase(tag.getName())){
                return true;
            }
        }
        return false;
    }
}
