package com.muzima.messaging.mms;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.mms.pdu_alt.RetrieveConf;
import com.muzima.messaging.exceptions.ApnUnavailableException;
import com.muzima.messaging.exceptions.MmsException;
import com.muzima.messaging.exceptions.MmsRadioException;

import java.io.IOException;

public interface IncomingMmsConnection {
    @Nullable
    RetrieveConf retrieve(@NonNull String contentLocation, byte[] transactionId, int subscriptionId) throws MmsException, MmsRadioException, ApnUnavailableException, IOException;

}
