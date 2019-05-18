package com.muzima.messaging.mms;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.muzima.messaging.attachments.Attachment;
import com.muzima.messaging.attachments.UriAttachment;
import com.muzima.messaging.sqlite.database.AttachmentDatabase;
import com.muzima.messaging.utils.Util;
import com.muzima.utils.MediaUtil;

import org.whispersystems.libsignal.util.guava.Optional;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public abstract class Slide {
    protected final Attachment attachment;
    protected final Context context;

    public Slide(@NonNull Context context, @NonNull Attachment attachment) {
        this.context = context;
        this.attachment = attachment;
    }

    public String getContentType() {
        return attachment.getContentType();
    }

    @Nullable
    public Uri getUri() {
        return attachment.getDataUri();
    }

    @Nullable
    public Uri getThumbnailUri() {
        return attachment.getThumbnailUri();
    }

    @NonNull
    public Optional<String> getBody() {
        return Optional.absent();
    }

    @NonNull
    public Optional<String> getCaption() {
        return Optional.fromNullable(attachment.getCaption());
    }

    @NonNull
    public Optional<String> getFileName() {
        return Optional.fromNullable(attachment.getFileName());
    }

    @Nullable
    public String getFastPreflightId() {
        return attachment.getFastPreflightId();
    }

    public long getFileSize() {
        return attachment.getSize();
    }

    public boolean hasImage() {
        return false;
    }

    public boolean hasVideo() {
        return false;
    }

    public boolean hasAudio() {
        return false;
    }

    public boolean hasDocument() {
        return false;
    }

    public boolean hasLocation() {
        return false;
    }

    public @NonNull
    String getContentDescription() {
        return "";
    }

    public Attachment asAttachment() {
        return attachment;
    }

    public boolean isInProgress() {
        return attachment.isInProgress();
    }

    public boolean isPendingDownload() {
        return getTransferState() == AttachmentDatabase.TRANSFER_PROGRESS_FAILED ||
                getTransferState() == AttachmentDatabase.TRANSFER_PROGRESS_PENDING;
    }

    public int getTransferState() {
        return attachment.getTransferState();
    }

    public @DrawableRes
    int getPlaceholderRes(Resources.Theme theme) {
        throw new AssertionError("getPlaceholderRes() called for non-drawable slide");
    }

    public boolean hasPlaceholder() {
        return false;
    }

    public boolean hasPlayOverlay() {
        return false;
    }

    protected static Attachment constructAttachmentFromUri(@NonNull Context context,
                                                           @NonNull Uri uri,
                                                           @NonNull String defaultMime,
                                                           long size,
                                                           int width,
                                                           int height,
                                                           boolean hasThumbnail,
                                                           @Nullable String fileName,
                                                           boolean voiceNote,
                                                           boolean quote) {
        try {
            String resolvedType = Optional.fromNullable(MediaUtil.getMimeType(context, uri)).or(defaultMime);
            String fastPreflightId = String.valueOf(SecureRandom.getInstance("SHA1PRNG").nextLong());
            return new UriAttachment(uri,
                    hasThumbnail ? uri : null,
                    resolvedType,
                    AttachmentDatabase.TRANSFER_PROGRESS_STARTED,
                    size,
                    width,
                    height,
                    fileName,
                    fastPreflightId,
                    voiceNote,
                    quote,
                    null);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (!(other instanceof Slide)) return false;

        Slide that = (Slide) other;

        return Util.equals(this.getContentType(), that.getContentType()) &&
                this.hasAudio() == that.hasAudio() &&
                this.hasImage() == that.hasImage() &&
                this.hasVideo() == that.hasVideo() &&
                this.getTransferState() == that.getTransferState() &&
                Util.equals(this.getUri(), that.getUri()) &&
                Util.equals(this.getThumbnailUri(), that.getThumbnailUri());
    }

    @Override
    public int hashCode() {
        return Util.hashCode(getContentType(), hasAudio(), hasImage(),
                hasVideo(), getUri(), getThumbnailUri(), getTransferState());
    }
}
