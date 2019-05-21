package com.muzima.messaging.crypto;

import android.content.Context;
import android.content.Intent;

import com.muzima.service.KeyCachingService;

public class SecurityEvent {
    public static final String SECURITY_UPDATE_EVENT = "com.muzima.KEY_EXCHANGE_UPDATE";

    public static void broadcastSecurityUpdateEvent(Context context) {
        Intent intent = new Intent(SECURITY_UPDATE_EVENT);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent, KeyCachingService.KEY_PERMISSION);
    }
}
