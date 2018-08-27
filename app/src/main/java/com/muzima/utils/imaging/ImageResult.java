/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils.imaging;

public final class ImageResult {

    private final String sectionName;
    private final String imageUri;
    private final String imageCaption;

    public ImageResult() {
        this(null, null, null);
    }

    public ImageResult(String sectionName, String imageUri, String imageCaption) {
        this.sectionName = sectionName;
        this.imageUri = imageUri;
        this.imageCaption = imageCaption;
    }

    public String getSectionName() {
        return sectionName;
    }

    public String getImageUri() {
        return imageUri;
    }

    public String getImageCaption() {
        return imageCaption;
    }

    @Override
    public String toString() {
        return "SectionName: " + sectionName + '\n' +
                "ImageUri: " + imageUri + '\n' +
                "ImageCaption: " + imageCaption + '\n';
    }
}