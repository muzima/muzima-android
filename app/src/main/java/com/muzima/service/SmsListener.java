package com.muzima.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.multidex.MultiDexApplication;
import android.telephony.SmsMessage;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.jobs.SmsReceiveJob;
import com.muzima.messaging.twofactoraunthentication.RegistrationActivity;
import com.muzima.messaging.utils.Util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsListener extends BroadcastReceiver {

    private static final String SMS_RECEIVED_ACTION  = Telephony.Sms.Intents.SMS_RECEIVED_ACTION;
    private static final String SMS_DELIVERED_ACTION = Telephony.Sms.Intents.SMS_DELIVER_ACTION;

    private static final Pattern CHALLENGE_PATTERN = Pattern.compile(".*Your (Signal|TextSecure) verification code:? ([0-9]{3,4})-([0-9]{3,4}).*", Pattern.DOTALL);

    private boolean isExemption(SmsMessage message, String messageBody) {

        // ignore CLASS0 ("flash") messages
        if (message.getMessageClass() == SmsMessage.MessageClass.CLASS_0)
            return true;

        // ignore OTP messages from Sparebank1 (Norwegian bank)
        if (messageBody.startsWith("Sparebank1://otp?")) {
            return true;
        }

        return
                message.getOriginatingAddress().length() < 7 &&
                        (messageBody.toUpperCase().startsWith("//ANDROID:") || // Sprint Visual Voicemail
                                messageBody.startsWith("//BREW:")); //BREW stands for “Binary Runtime Environment for Wireless"
    }

    private SmsMessage getSmsMessageFromIntent(Intent intent) {
        Bundle bundle = intent.getExtras();
        Object[] pdus = (Object[])bundle.get("pdus");

        if (pdus == null || pdus.length == 0)
            return null;

        return SmsMessage.createFromPdu((byte[])pdus[0]);
    }

    private String getSmsMessageBodyFromIntent(Intent intent) {
        Bundle bundle = intent.getExtras();
        Object[] pdus = (Object[])bundle.get("pdus");
        StringBuilder bodyBuilder = new StringBuilder();

        if (pdus == null)
            return null;

        for (Object pdu : pdus)
            bodyBuilder.append(SmsMessage.createFromPdu((byte[])pdu).getDisplayMessageBody());

        return bodyBuilder.toString();
    }

    private boolean isRelevant(Context context, Intent intent) {
        SmsMessage message = getSmsMessageFromIntent(intent);
        String messageBody = getSmsMessageBodyFromIntent(intent);

        if (message == null && messageBody == null)
            return false;

        if (isExemption(message, messageBody))
            return false;

        if (!ApplicationMigrationService.isDatabaseImported(context))
            return false;

        if (isChallenge(context, messageBody))
            return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                SMS_RECEIVED_ACTION.equals(intent.getAction()) &&
                Util.isDefaultSmsProvider(context))
        {
            return false;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT &&
                TextSecurePreferences.isInterceptAllSmsEnabled(context))
        {
            return true;
        }

        return false;
    }

    @VisibleForTesting
    boolean isChallenge(@NonNull Context context, @Nullable String messageBody) {
        if (messageBody == null)
            return false;

        if (CHALLENGE_PATTERN.matcher(messageBody).matches() &&
                TextSecurePreferences.isVerifying(context))
        {
            return true;
        }

        return false;
    }

    @VisibleForTesting String parseChallenge(String messageBody) {
        Matcher challengeMatcher = CHALLENGE_PATTERN.matcher(messageBody);

        if (!challengeMatcher.matches()) {
            throw new AssertionError("Expression should match.");
        }

        return challengeMatcher.group(2) + challengeMatcher.group(3);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("SMSListener", "Got SMS broadcast...");

        String messageBody = getSmsMessageBodyFromIntent(intent);
        if (SMS_RECEIVED_ACTION.equals(intent.getAction()) && isChallenge(context, messageBody)) {
            Log.w("SmsListener", "Got challenge!");
            Intent challengeIntent = new Intent(RegistrationActivity.CHALLENGE_EVENT);
            challengeIntent.putExtra(RegistrationActivity.CHALLENGE_EXTRA, parseChallenge(messageBody));
            context.sendBroadcast(challengeIntent);

            abortBroadcast();
        } else if ((intent.getAction().equals(SMS_DELIVERED_ACTION)) ||
                (intent.getAction().equals(SMS_RECEIVED_ACTION)) && isRelevant(context, intent))
        {
            Log.i("SmsListener", "Constructing SmsReceiveJob...");
            Object[] pdus = (Object[]) intent.getExtras().get("pdus");
            int subscriptionId = intent.getExtras().getInt("subscription", -1);

            MuzimaApplication.getInstance(context).getJobManager().add(new SmsReceiveJob(context, pdus, subscriptionId));

            abortBroadcast();
        }
    }
}
