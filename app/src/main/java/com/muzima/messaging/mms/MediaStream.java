package com.muzima.messaging.mms;

import java.io.InputStream;

public class MediaStream {
    private final InputStream stream;
    private final String mimeType;
    private final int width;
    private final int height;

    public MediaStream(InputStream stream, String mimeType, int width, int height) {
        this.stream = stream;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
    }

    public InputStream getStream() {
        return stream;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
