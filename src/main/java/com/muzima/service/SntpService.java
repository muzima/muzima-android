package com.muzima.service;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class SntpService {

    public Date getLocalTime() {
        long nowAsPerDeviceTimeZone = 0;
        SntpClient sntpClient = new SntpClient();

        if (sntpClient.requestTime("0.africa.pool.ntp.org", 30000)) {
            nowAsPerDeviceTimeZone = sntpClient.getNtpTime();
        }
        return new Date(nowAsPerDeviceTimeZone);
    }
}
