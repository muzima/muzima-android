package com.muzima.messaging.dependencies;

import android.content.Context;
import android.util.Log;

import com.muzima.BuildConfig;
import com.muzima.messaging.CreateProfileActivity;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.crypto.storage.SignalProtocolStoreImpl;
import com.muzima.messaging.events.ReminderUpdateEvent;
import com.muzima.messaging.jobs.CleanPreKeysJob;
import com.muzima.messaging.jobs.CreateSignedPreKeyJob;
import com.muzima.messaging.jobs.GcmRefreshJob;
import com.muzima.messaging.jobs.MultiDeviceContactUpdateJob;
import com.muzima.messaging.jobs.MultiDeviceReadUpdateJob;
import com.muzima.messaging.jobs.PushGroupSendJob;
import com.muzima.messaging.jobs.PushMediaSendJob;
import com.muzima.messaging.jobs.PushNotificationReceiveJob;
import com.muzima.messaging.jobs.PushTextSendJob;
import com.muzima.messaging.jobs.RefreshPreKeysJob;
import com.muzima.messaging.jobs.RotateCertificateJob;
import com.muzima.messaging.jobs.RotateSignedPreKeyJob;
import com.muzima.messaging.jobs.SendReadReceiptJob;
import com.muzima.messaging.push.SecurityEventListener;
import com.muzima.messaging.push.SignalServiceNetworkAccess;
import com.muzima.service.IncomingMessageObserver;

import org.greenrobot.eventbus.EventBus;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.api.util.RealtimeSleepTimer;
import org.whispersystems.signalservice.api.util.SleepTimer;
import org.whispersystems.signalservice.api.util.UptimeSleepTimer;
import org.whispersystems.signalservice.api.websocket.ConnectivityListener;

import dagger.Module;
import dagger.Provides;

@Module(complete = false, injects = {CleanPreKeysJob.class,
        CreateSignedPreKeyJob.class,
        PushGroupSendJob.class,
        PushTextSendJob.class,
        PushMediaSendJob.class,
        RefreshPreKeysJob.class,
        IncomingMessageObserver.class,
        PushNotificationReceiveJob.class,
        MultiDeviceContactUpdateJob.class,
        MultiDeviceReadUpdateJob.class,
        GcmRefreshJob.class,
        RotateSignedPreKeyJob.class,
        CreateProfileActivity.class,
        SendReadReceiptJob.class,
        RotateCertificateJob.class})
public class SignalCommunicationModule {
    private static final String TAG = SignalCommunicationModule.class.getSimpleName();

    private final Context context;
    private final SignalServiceNetworkAccess networkAccess;

    private SignalServiceAccountManager accountManager;
    private SignalServiceMessageSender messageSender;
    private SignalServiceMessageReceiver messageReceiver;

    public SignalCommunicationModule(Context context, SignalServiceNetworkAccess networkAccess) {
        this.context       = context;
        this.networkAccess = networkAccess;
    }

    @Provides
    synchronized SignalServiceAccountManager provideSignalAccountManager() {
        if (this.accountManager == null) {
            this.accountManager = new SignalServiceAccountManager(networkAccess.getConfiguration(context),
                    new DynamicCredentialsProvider(context),
                    BuildConfig.USER_AGENT);
        }

        return this.accountManager;
    }

    @Provides
    synchronized SignalServiceMessageSender provideSignalMessageSender() {
        if (this.messageSender == null) {
            this.messageSender = new SignalServiceMessageSender(networkAccess.getConfiguration(context),
                    new DynamicCredentialsProvider(context),
                    new SignalProtocolStoreImpl(context),
                    BuildConfig.USER_AGENT,
                    TextSecurePreferences.isMultiDevice(context),
                    Optional.fromNullable(IncomingMessageObserver.getPipe()),
                    Optional.fromNullable(IncomingMessageObserver.getUnidentifiedPipe()),
                    Optional.of(new SecurityEventListener(context)));
        } else {
            this.messageSender.setMessagePipe(IncomingMessageObserver.getPipe(), IncomingMessageObserver.getUnidentifiedPipe());
            this.messageSender.setIsMultiDevice(TextSecurePreferences.isMultiDevice(context));
        }

        return this.messageSender;
    }

    @Provides
    synchronized SignalServiceMessageReceiver provideSignalMessageReceiver() {
        if (this.messageReceiver == null) {
            SleepTimer sleepTimer =  TextSecurePreferences.isGcmDisabled(context) ? new RealtimeSleepTimer(context) : new UptimeSleepTimer();

            this.messageReceiver = new SignalServiceMessageReceiver(networkAccess.getConfiguration(context),
                    new DynamicCredentialsProvider(context),
                    BuildConfig.USER_AGENT,
                    new PipeConnectivityListener(),
                    sleepTimer);
        }

        return this.messageReceiver;
    }

    @Provides
    synchronized SignalServiceNetworkAccess provideSignalServiceNetworkAccess() {
        return networkAccess;
    }

    private static class DynamicCredentialsProvider implements CredentialsProvider {

        private final Context context;

        private DynamicCredentialsProvider(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        public String getUser() {
            return TextSecurePreferences.getLocalNumber(context);
        }

        @Override
        public String getPassword() {
            return TextSecurePreferences.getPushServerPassword(context);
        }

        @Override
        public String getSignalingKey() {
            return TextSecurePreferences.getSignalingKey(context);
        }
    }

    private class PipeConnectivityListener implements ConnectivityListener {

        @Override
        public void onConnected() {
            Log.i(TAG, "onConnected()");
        }

        @Override
        public void onConnecting() {
            Log.i(TAG, "onConnecting()");
        }

        @Override
        public void onDisconnected() {
            Log.w(TAG, "onDisconnected()");
        }

        @Override
        public void onAuthenticationFailure() {
            Log.w(TAG, "onAuthenticationFailure()");
            TextSecurePreferences.setUnauthorizedReceived(context, true);
            EventBus.getDefault().post(new ReminderUpdateEvent());
        }

    }
}
