package com.muzima.service;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class SntpService {

        public Date getUTCTime(){
        long nowAsPerDeviceTimeZone = 0;
        SntpClient sntpClient = new SntpClient();

        if (sntpClient.requestTime("0.africa.pool.ntp.org", 30000)) {
            nowAsPerDeviceTimeZone = sntpClient.getNtpTime();
            Calendar cal = Calendar.getInstance();
            TimeZone timeZoneInDevice = cal.getTimeZone();
            int differentialOfTimeZones = timeZoneInDevice.getOffset(System.currentTimeMillis());
            nowAsPerDeviceTimeZone -= differentialOfTimeZones;
        }
        return new Date(nowAsPerDeviceTimeZone);
    }
}
