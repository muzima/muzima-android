package com.muzima.messaging.mms;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import android.util.Log;

import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.pdu_alt.NotifyRespInd;
import com.google.android.mms.pdu_alt.PduComposer;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduParser;
import com.google.android.mms.pdu_alt.RetrieveConf;
import com.muzima.messaging.exceptions.MmsException;
import com.muzima.messaging.provider.MmsBodyProvider;
import com.muzima.messaging.utils.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class IncomingLollipopMmsConnection extends LollipopMmsConnection implements IncomingMmsConnection {

    public static final String ACTION = IncomingLollipopMmsConnection.class.getCanonicalName() + "MMS_DOWNLOADED_ACTION";
    private static final String TAG = IncomingLollipopMmsConnection.class.getSimpleName();

    public IncomingLollipopMmsConnection(Context context) {
        super(context, ACTION);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public synchronized void onResult(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.i(TAG, "HTTP status: " + intent.getIntExtra(SmsManager.EXTRA_MMS_HTTP_STATUS, -1));
        }
        Log.i(TAG, "code: " + getResultCode() + ", result string: " + getResultData());
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public synchronized @Nullable
    RetrieveConf retrieve(@NonNull String contentLocation,
                          byte[] transactionId,
                          int subscriptionId) throws MmsException {
        beginTransaction();

        try {
            MmsBodyProvider.Pointer pointer = MmsBodyProvider.makeTemporaryPointer(getContext());

            Log.i(TAG, "downloading multimedia from " + contentLocation + " to " + pointer.getUri());

            SmsManager smsManager;

            if (Build.VERSION.SDK_INT >= 22 && subscriptionId != -1) {
                smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
            } else {
                smsManager = SmsManager.getDefault();
            }

            smsManager.downloadMultimediaMessage(getContext(),
                    contentLocation,
                    pointer.getUri(),
                    null,
                    getPendingIntent());

            waitForResult();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Util.copy(pointer.getInputStream(), baos);
            pointer.close();

            Log.i(TAG, baos.size() + "-byte response: ");// + Hex.dump(baos.toByteArray()));

            RetrieveConf retrieved = (RetrieveConf) new PduParser(baos.toByteArray()).parse();

            if (retrieved == null) return null;

            sendRetrievedAcknowledgement(transactionId, retrieved.getMmsVersion(), subscriptionId);
            return retrieved;
        } catch (IOException | TimeoutException e) {
            Log.w(TAG, e);
            throw new MmsException(e);
        } finally {
            endTransaction();
        }
    }

    private void sendRetrievedAcknowledgement(byte[] transactionId, int mmsVersion, int subscriptionId) {
        try {
            NotifyRespInd retrieveResponse = new NotifyRespInd(mmsVersion, transactionId, PduHeaders.STATUS_RETRIEVED);
            new OutgoingLollipopMmsConnection(getContext()).send(new PduComposer(getContext(), retrieveResponse).make(), subscriptionId);
        } catch (InvalidHeaderValueException e) {
            Log.w(TAG, e);
        }
    }
}
