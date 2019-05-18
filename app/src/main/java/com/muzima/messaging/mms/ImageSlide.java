package com.muzima.messaging.mms;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.muzima.R;
import com.muzima.messaging.attachments.Attachment;
import com.muzima.utils.MediaUtil;

public class ImageSlide extends Slide {

    @SuppressWarnings("unused")
    private static final String TAG = ImageSlide.class.getSimpleName();

    public ImageSlide(@NonNull Context context, @NonNull Attachment attachment) {
        super(context, attachment);
    }

    public ImageSlide(Context context, Uri uri, long size, int width, int height) {
        super(context, constructAttachmentFromUri(context, uri, MediaUtil.IMAGE_JPEG, size, width, height, true, null, false, false));
    }

    @Override
    public @DrawableRes
    int getPlaceholderRes(Resources.Theme theme) {
        return 0;
    }

    @Override
    public @Nullable
    Uri getThumbnailUri() {
        return getUri();
    }

    @Override
    public boolean hasImage() {
        return true;
    }

    @NonNull
    @Override
    public String getContentDescription() {
        return context.getString(R.string.slide_image);
    }
}
