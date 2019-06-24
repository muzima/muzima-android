package com.muzima.messaging.contacts.avatars;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.utils.Conversions;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.MessageDigest;

public class SystemContactPhoto implements ContactPhoto {

    private final @NonNull SignalAddress address;
    private final @NonNull Uri contactPhotoUri;
    private final long lastModifiedTime;

    public SystemContactPhoto(@NonNull SignalAddress address, @NonNull Uri contactPhotoUri, long lastModifiedTime) {
        this.address = address;
        this.contactPhotoUri = contactPhotoUri;
        this.lastModifiedTime = lastModifiedTime;
    }

    @Override
    public InputStream openInputStream(Context context) throws FileNotFoundException {
        return context.getContentResolver().openInputStream(contactPhotoUri);
    }

    @Nullable
    @Override
    public Uri getUri(@NonNull Context context) {
        return contactPhotoUri;
    }

    @Override
    public boolean isProfilePhoto() {
        return false;
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        messageDigest.update(address.serialize().getBytes());
        messageDigest.update(contactPhotoUri.toString().getBytes());
        messageDigest.update(Conversions.longToByteArray(lastModifiedTime));
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof SystemContactPhoto)) return false;

        SystemContactPhoto that = (SystemContactPhoto)other;

        return this.address.equals(that.address) && this.contactPhotoUri.equals(that.contactPhotoUri) && this.lastModifiedTime == that.lastModifiedTime;
    }

    @Override
    public int hashCode() {
        return address.hashCode() ^ contactPhotoUri.hashCode() ^ (int)lastModifiedTime;
    }
}
