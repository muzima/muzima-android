package com.muzima.model;

import android.support.annotation.NonNull;

public class MessageResult {
    public final SignalRecipient recipient;
    public final String bodySnippet;
    public final long threadId;
    public final long receivedTimestampMs;

    public MessageResult(@NonNull SignalRecipient recipient,
                         @NonNull String bodySnippet,
                         long threadId,
                         long receivedTimestampMs) {
        this.recipient = recipient;
        this.bodySnippet = bodySnippet;
        this.threadId = threadId;
        this.receivedTimestampMs = receivedTimestampMs;
    }
}
