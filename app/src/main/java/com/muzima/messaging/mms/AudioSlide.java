package com.muzima.messaging.mms;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.muzima.R;
import com.muzima.messaging.attachments.Attachment;
import com.muzima.messaging.attachments.UriAttachment;
import com.muzima.messaging.sqlite.database.AttachmentDatabase;
import com.muzima.utils.MediaUtil;
import com.muzima.utils.ResUtil;

public class AudioSlide extends Slide {
    public AudioSlide(Context context, Uri uri, long dataSize, boolean voiceNote) {
        super(context, constructAttachmentFromUri(context, uri, MediaUtil.AUDIO_UNSPECIFIED, dataSize, 0, 0, false, null, voiceNote, false));
    }

    public AudioSlide(Context context, Uri uri, long dataSize, String contentType, boolean voiceNote) {
        super(context,  new UriAttachment(uri, null, contentType, AttachmentDatabase.TRANSFER_PROGRESS_STARTED, dataSize, 0, 0, null, null, voiceNote, false, null));
    }

    public AudioSlide(Context context, Attachment attachment) {
        super(context, attachment);
    }

    @Override
    @Nullable
    public Uri getThumbnailUri() {
        return null;
    }

    @Override
    public boolean hasPlaceholder() {
        return true;
    }

    @Override
    public boolean hasImage() {
        return true;
    }

    @Override
    public boolean hasAudio() {
        return true;
    }

    @NonNull
    @Override
    public String getContentDescription() {
        return context.getString(R.string.slide_audio);
    }

    @Override
    public @DrawableRes
    int getPlaceholderRes(Resources.Theme theme) {
        return ResUtil.getDrawableRes(theme, R.attr.conversation_icon_attach_audio);
    }
}
