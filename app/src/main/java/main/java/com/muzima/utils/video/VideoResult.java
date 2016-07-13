/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
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
        StringBuilder dialogText = new StringBuilder(255);
        dialogText.append("SectionName: ").append(sectionName).append('\n');
        dialogText.append("VideoUri: ").append(videoUri).append('\n');
        dialogText.append("VideoCaption: ").append(videoCaption).append('\n');
        return dialogText.toString();
    }
}