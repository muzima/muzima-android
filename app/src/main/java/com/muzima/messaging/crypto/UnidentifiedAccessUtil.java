package com.muzima.messaging.crypto;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.muzima.BuildConfig;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.utils.Util;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.Base64;

import org.signal.libsignal.metadata.certificate.InvalidCertificateException;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECPublicKey;

import org.signal.libsignal.metadata.certificate.CertificateValidator;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.crypto.UnidentifiedAccess;
import org.whispersystems.signalservice.api.crypto.UnidentifiedAccessPair;

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

    @WorkerThread
    public static Optional<UnidentifiedAccessPair> getAccessFor(@NonNull Context context,
                                                                @NonNull SignalRecipient recipient)
    {
        if (!TextSecurePreferences.isUnidentifiedDeliveryEnabled(context)) {
            Log.i(TAG, "Unidentified delivery is disabled. [other]");
            return Optional.absent();
        }

        try {
            byte[] theirUnidentifiedAccessKey = getTargetUnidentifiedAccessKey(recipient);
            byte[] ourUnidentifiedAccessKey = getSelfUnidentifiedAccessKey(context);
            byte[] ourUnidentifiedAccessCertificate = TextSecurePreferences.getUnidentifiedAccessCertificate(context);

            if (TextSecurePreferences.isUniversalUnidentifiedAccess(context)) {
                ourUnidentifiedAccessKey = Util.getSecretBytes(16);
            }

            Log.i(TAG, "Their access key present? " + (theirUnidentifiedAccessKey != null) +
                    " | Our access key present? " + (ourUnidentifiedAccessKey != null) +
                    " | Our certificate present? " + (ourUnidentifiedAccessCertificate != null));

            if (theirUnidentifiedAccessKey != null &&
                    ourUnidentifiedAccessKey != null   &&
                    ourUnidentifiedAccessCertificate != null)
            {
                return Optional.of(new UnidentifiedAccessPair(new UnidentifiedAccess(theirUnidentifiedAccessKey,
                        ourUnidentifiedAccessCertificate),
                        new UnidentifiedAccess(ourUnidentifiedAccessKey,
                                ourUnidentifiedAccessCertificate)));
            }

            return Optional.absent();
        } catch (InvalidCertificateException e) {
            Log.w(TAG, e);
            return Optional.absent();
        }
    }

    public static Optional<UnidentifiedAccessPair> getAccessForSync(@NonNull Context context) {
        if (!TextSecurePreferences.isUnidentifiedDeliveryEnabled(context)) {
            Log.i(TAG, "Unidentified delivery is disabled. [self]");
            return Optional.absent();
        }

        try {
            byte[] ourUnidentifiedAccessKey = getSelfUnidentifiedAccessKey(context);
            byte[] ourUnidentifiedAccessCertificate = TextSecurePreferences.getUnidentifiedAccessCertificate(context);

            if (TextSecurePreferences.isUniversalUnidentifiedAccess(context)) {
                ourUnidentifiedAccessKey = Util.getSecretBytes(16);
            }

            if (ourUnidentifiedAccessKey != null && ourUnidentifiedAccessCertificate != null) {
                return Optional.of(new UnidentifiedAccessPair(new UnidentifiedAccess(ourUnidentifiedAccessKey,
                        ourUnidentifiedAccessCertificate),
                        new UnidentifiedAccess(ourUnidentifiedAccessKey,
                                ourUnidentifiedAccessCertificate)));
            }

            return Optional.absent();
        } catch (InvalidCertificateException e) {
            Log.w(TAG, e);
            return Optional.absent();
        }
    }

    public static @NonNull byte[] getSelfUnidentifiedAccessKey(@NonNull Context context) {
        return UnidentifiedAccess.deriveAccessKeyFrom(ProfileKeyUtil.getProfileKey(context));
    }

    private static @Nullable
    byte[] getTargetUnidentifiedAccessKey(@NonNull SignalRecipient recipient) {
        byte[] theirProfileKey = recipient.resolve().getProfileKey();

        switch (recipient.resolve().getUnidentifiedAccessMode()) {
            case UNKNOWN:
                if (theirProfileKey == null) return Util.getSecretBytes(16);
                else return UnidentifiedAccess.deriveAccessKeyFrom(theirProfileKey);
            case DISABLED:
                return null;
            case ENABLED:
                if (theirProfileKey == null) return null;
                else return UnidentifiedAccess.deriveAccessKeyFrom(theirProfileKey);
            case UNRESTRICTED:
                return Util.getSecretBytes(16);
            default:
                throw new AssertionError("Unknown mode: " + recipient.getUnidentifiedAccessMode().getMode());
        }
    }

}
