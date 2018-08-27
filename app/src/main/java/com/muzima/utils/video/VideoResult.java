/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils.video;

public final class VideoResult {

    private final String sectionName;
    private final String videoUri;
    private final String videoCaption;

    public VideoResult() {
        this(null, null, null);
    }

    public VideoResult(String sectionName, String videoUri, String videoCaption) {
        this.sectionName = sectionName;
        this.videoUri = videoUri;
        this.videoCaption = videoCaption;
    }

    public String getSectionName() {
        return sectionName;
    }

    public String getVideoUri() {
        return videoUri;
    }

    public String getVideoCaption() {
        return videoCaption;
    }

    @Override
    public String toString() {
        return "SectionName: " + sectionName + '\n' +
                "VideoUri: " + videoUri + '\n' +
                "VideoCaption: " + videoCaption + '\n';
    }
}