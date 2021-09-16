/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

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
