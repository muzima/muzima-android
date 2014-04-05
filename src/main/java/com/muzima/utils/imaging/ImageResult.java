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
        StringBuilder dialogText = new StringBuilder(255);
        dialogText.append("SectionName: ").append(sectionName).append('\n');
        dialogText.append("ImageUri: ").append(imageUri).append('\n');
        dialogText.append("ImageCaption: ").append(imageCaption).append('\n');
        return dialogText.toString();
    }

}