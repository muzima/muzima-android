package com.muzima.messaging.crypto;

import android.content.Context;
import android.support.annotation.NonNull;

import com.muzima.BuildConfig;
import com.muzima.utils.Base64;

import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECPublicKey;

import org.signal.libsignal.metadata.certificate.CertificateValidator;

import org.whispersystems.signalservice.api.crypto.UnidentifiedAccess;

import java.io.IOException;

public class UnidentifiedAccessUtil {
    private static final String TAG = UnidentifiedAccessUtil.class.getSimpleName();

    public static CertificateValidator getCertificateValidator() {
        try {
            ECPublicKey unidentifiedSenderTrustRoot = Curve.decodePoint(Base64.decode(BuildConfig.UNIDENTIFIED_SENDER_TRUST_ROOT), 0);
            return new CertificateValidator(unidentifiedSenderTrustRoot);
        } catch (InvalidKeyException | IOException e) {
            throw new AssertionError(e);
        }
    }

    public static @NonNull byte[] getSelfUnidentifiedAccessKey(@NonNull Context context) {
        return UnidentifiedAccess.deriveAccessKeyFrom(ProfileKeyUtil.getProfileKey(context));
    }

}
