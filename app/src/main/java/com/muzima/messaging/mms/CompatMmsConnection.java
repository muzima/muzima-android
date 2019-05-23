package com.muzima.messaging.mms;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.RetrieveConf;
import com.google.android.mms.pdu_alt.SendConf;
import com.muzima.messaging.exceptions.ApnUnavailableException;
import com.muzima.messaging.exceptions.MmsException;
import com.muzima.messaging.exceptions.MmsRadioException;

import java.io.IOException;

public class CompatMmsConnection implements OutgoingMmsConnection, IncomingMmsConnection {
    private static final String TAG = CompatMmsConnection.class.getSimpleName();

    private Context context;

    public CompatMmsConnection(Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public SendConf send(@NonNull byte[] pduBytes, int subscriptionId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.i(TAG, "Sending via Lollipop API");
            return new OutgoingLollipopMmsConnection(context).send(pduBytes, subscriptionId);
        }

        if (subscriptionId == -1) {
            Log.i(TAG, "Sending via legacy connection");
            try {
                SendConf result = new OutgoingLegacyMmsConnection(context).send(pduBytes, subscriptionId);

                if (result != null && result.getResponseStatus() == PduHeaders.RESPONSE_STATUS_OK) {
                    return result;
                } else {
                    Log.w(TAG, "Got bad legacy response: " + (result != null ? result.getResponseStatus() : null));
                }
            } catch (ApnUnavailableException e) {
                Log.w(TAG, e);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.i(TAG, "Falling back to sending via Lollipop API");
            return new OutgoingLollipopMmsConnection(context).send(pduBytes, subscriptionId);
        }

        throw new AssertionError("Both lollipop and legacy connections failed...");
    }

    @Nullable
    @Override
    public RetrieveConf retrieve(@NonNull String contentLocation,
                                 byte[] transactionId,
                                 int subscriptionId)
            throws MmsException, MmsRadioException, ApnUnavailableException, IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.i(TAG, "Receiving via Lollipop API");
            try {
                return new IncomingLollipopMmsConnection(context).retrieve(contentLocation, transactionId, subscriptionId);
            } catch (MmsException e) {
                Log.w(TAG, e);
            }

            Log.i(TAG, "Falling back to receiving via legacy connection");
        }

        if (Build.VERSION.SDK_INT < 22 || subscriptionId == -1) {
            Log.i(TAG, "Receiving via legacy API");
            try {
                return new IncomingLegacyMmsConnection(context).retrieve(contentLocation, transactionId, subscriptionId);
            } catch (MmsRadioException | ApnUnavailableException | IOException e) {
                Log.w(TAG, e);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.i(TAG, "Falling back to receiving via Lollipop API");
            return new IncomingLollipopMmsConnection(context).retrieve(contentLocation, transactionId, subscriptionId);
        }

        throw new IOException("Both lollipop and fallback APIs failed...");
    }
}
