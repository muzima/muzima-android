package com.muzima.messaging.mms;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import com.muzima.R;
import com.muzima.messaging.attachments.Attachment;
import com.muzima.utils.MediaUtil;
import com.muzima.utils.ResUtil;

public class VideoSlide extends Slide {

    public VideoSlide(Context context, Uri uri, long dataSize) {
        super(context, constructAttachmentFromUri(context, uri, MediaUtil.VIDEO_UNSPECIFIED, dataSize, 0, 0, MediaUtil.hasVideoThumbnail(uri), null, false, false));
    }

    public VideoSlide(Context context, Attachment attachment) {
        super(context, attachment);
    }

    @Override
    public boolean hasPlaceholder() {
        return true;
    }

    @Override
    public boolean hasPlayOverlay() {
        return true;
    }

    @Override
    public @DrawableRes
    int getPlaceholderRes(Resources.Theme theme) {
        return ResUtil.getDrawableRes(theme, R.attr.conversation_icon_attach_video);
    }

    @Override
    public boolean hasImage() {
        return true;
    }

    @Override
    public boolean hasVideo() {
        return true;
    }

    @NonNull
    @Override
    public String getContentDescription() {
        return context.getString(R.string.general_video);
    }
}
