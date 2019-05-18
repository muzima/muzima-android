package com.muzima.messaging;

import com.muzima.model.SignalRecipient;

public interface RecipientModifiedListener {
    void onModified(SignalRecipient recipient);
}
