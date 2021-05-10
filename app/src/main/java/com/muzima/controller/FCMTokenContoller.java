package com.muzima.controller;

import android.util.Log;

import com.muzima.api.service.NotificationTokenService;

import java.io.IOException;

public class FCMTokenContoller {
    private final NotificationTokenService notificationTokenService;

    public FCMTokenContoller(NotificationTokenService notificationTokenService) {
        this.notificationTokenService = notificationTokenService;
    }

    public boolean sendTokenToServer(String token,String userSystemId) throws IOException {
        return notificationTokenService.sendTokenToServer(token, userSystemId);
    }
}
