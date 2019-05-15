package com.muzima.messaging.crypto;

import android.content.Context;
import android.support.annotation.NonNull;

import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.utils.Util;
import com.muzima.utils.Base64;

import java.io.IOException;

public class ProfileKeyUtil {
    public static synchronized boolean hasProfileKey(@NonNull Context context) {
        return TextSecurePreferences.getProfileKey(context) != null;
    }

    public static synchronized @NonNull byte[] getProfileKey(@NonNull Context context) {
        try {
            String encodedProfileKey = TextSecurePreferences.getProfileKey(context);

            if (encodedProfileKey == null) {
                encodedProfileKey = Util.getSecret(32);
                TextSecurePreferences.setProfileKey(context, encodedProfileKey);
            }

            return Base64.decode(encodedProfileKey);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static synchronized @NonNull byte[] rotateProfileKey(@NonNull Context context) {
        TextSecurePreferences.setProfileKey(context, null);
        return getProfileKey(context);
    }
}
