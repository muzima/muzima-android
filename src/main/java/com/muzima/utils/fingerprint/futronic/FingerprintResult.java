package com.muzima.utils.fingerprint.futronic;

public final class FingerprintResult {
    private final String sectionName;
    private final String fingerprintString;

    public FingerprintResult() {
        this(null, null);
    }

    public FingerprintResult(String fingerprint, String sectionName) {
        this.fingerprintString = fingerprint;
        this.sectionName = sectionName;
    }

    public String getSectionName() {
        return sectionName;
    }

    public String getFingerprintString() {
        return fingerprintString;
    }
}