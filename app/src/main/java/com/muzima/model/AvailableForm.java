/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.model;

import com.muzima.api.model.Tag;
import com.muzima.utils.Constants;

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
    public boolean hasDescriminator(String discriminator){
        switch (discriminator) {
            case Constants.FORM_JSON_DISCRIMINATOR_REGISTRATION:
            case Constants.FORM_DISCRIMINATOR_REGISTRATION:
                return Constants.FORM_DISCRIMINATOR_REGISTRATION.equals(getDiscriminator())
                        || Constants.FORM_JSON_DISCRIMINATOR_REGISTRATION.equals(getDiscriminator());
            case Constants.FORM_JSON_DISCRIMINATOR_ENCOUNTER:
            case Constants.FORM_XML_DISCRIMINATOR_ENCOUNTER:
                return Constants.FORM_JSON_DISCRIMINATOR_ENCOUNTER.equals(getDiscriminator())
                        || Constants.FORM_XML_DISCRIMINATOR_ENCOUNTER.equals(getDiscriminator());
        }
        return false;
    }
}
