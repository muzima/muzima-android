package com.muzima.messaging.crypto;

import android.content.Context;
import android.support.annotation.NonNull;

import com.muzima.messaging.crypto.storage.TextSecureSessionStore;
import com.muzima.messaging.sqlite.database.SignalAddress;

import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.SessionStore;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;

public class SessionUtil {
    public static boolean hasSession(Context context, @NonNull SignalAddress address) {
        SessionStore sessionStore   = new TextSecureSessionStore(context);
        SignalProtocolAddress axolotlAddress = new SignalProtocolAddress(address.serialize(), SignalServiceAddress.DEFAULT_DEVICE_ID);

        return sessionStore.containsSession(axolotlAddress);
    }

    public static void archiveSiblingSessions(Context context, SignalProtocolAddress address) {
        TextSecureSessionStore  sessionStore = new TextSecureSessionStore(context);
        sessionStore.archiveSiblingSessions(address);
    }

    public static void archiveAllSessions(Context context) {
        new TextSecureSessionStore(context).archiveAllSessions();
    }

}
