/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.model;

import com.muzima.api.model.Tag;
import com.muzima.utils.Constants;

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

    public boolean isRegistrationForm(){
        return Constants.FORM_DISCRIMINATOR_REGISTRATION.equalsIgnoreCase(getDiscriminator()) ||
                Constants.FORM_JSON_DISCRIMINATOR_REGISTRATION.equalsIgnoreCase(getDiscriminator()) ||
                Constants.FORM_JSON_DISCRIMINATOR_GENERIC_REGISTRATION.equalsIgnoreCase(getDiscriminator()) ||
                Constants.FORM_JSON_DISCRIMINATOR_SHR_REGISTRATION.equalsIgnoreCase(getDiscriminator());
    }

    public boolean isProviderReport(){
        return Constants.FORM_DISCRIMINATOR_PROVIDER_REPORT.equalsIgnoreCase(getDiscriminator());
    }
}
