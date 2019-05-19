package com.muzima.messaging.typingstatus;

import android.telephony.SmsMessage;
import android.util.Log;

import com.muzima.messaging.utils.CharacterCalculator;

public class SmsCharacterCalculator extends CharacterCalculator {

    private static final String TAG = SmsCharacterCalculator.class.getSimpleName();

    @Override
    public CharacterState calculateCharacters(String messageBody) {
        int[] length;
        int   messagesSpent;
        int   charactersSpent;
        int   charactersRemaining;

        try {
            length              = SmsMessage.calculateLength(messageBody, false);
            messagesSpent       = length[0];
            charactersSpent     = length[1];
            charactersRemaining = length[2];
        } catch (NullPointerException e) {
            Log.w(TAG, e);
            messagesSpent       = 1;

            charactersSpent     = messageBody.length();
            charactersRemaining = 1000;
        }

        int maxMessageSize;

        if (messagesSpent > 0) {
            maxMessageSize = (charactersSpent + charactersRemaining) / messagesSpent;
        } else {
            maxMessageSize = (charactersSpent + charactersRemaining);
        }

        return new CharacterState(messagesSpent, charactersRemaining, maxMessageSize);
    }
}
