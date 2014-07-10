package com.muzima.utils.audio;

public final class AudioResult {

    private final String sectionName;
    private final String audioUri;
    private final String audioCaption;

    public AudioResult() {
        this(null, null, null);
    }

    public AudioResult(String sectionName, String audioUri, String audioCaption) {
        this.sectionName = sectionName;
        this.audioUri = audioUri;
        this.audioCaption = audioCaption;
    }

    public String getSectionName() {
        return sectionName;
    }

    public String getAudioUri() {
        return audioUri;
    }

    public String getAudioCaption() {
        return audioCaption;
    }

    @Override
    public String toString() {
        StringBuilder dialogText = new StringBuilder(255);
        dialogText.append("SectionName: ").append(sectionName).append('\n');
        dialogText.append("AudioUri: ").append(audioUri).append('\n');
        dialogText.append("AudioCaption: ").append(audioCaption).append('\n');
        return dialogText.toString();
    }
}