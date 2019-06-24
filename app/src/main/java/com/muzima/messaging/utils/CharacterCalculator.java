package com.muzima.messaging.utils;

import android.os.Parcel;
import android.support.annotation.NonNull;

import com.muzima.messaging.typingstatus.MmsCharacterCalculator;
import com.muzima.messaging.typingstatus.PushCharacterCalculator;
import com.muzima.messaging.typingstatus.SmsCharacterCalculator;

public abstract class CharacterCalculator {
    public abstract CharacterState calculateCharacters(String messageBody);

    public static CharacterCalculator readFromParcel(@NonNull Parcel in) {
        switch (in.readInt()) {
            case 1:
                 return new SmsCharacterCalculator();
            case 2:
                 return new MmsCharacterCalculator();
            case 3:
                 return new PushCharacterCalculator();
            default:
                 throw new IllegalArgumentException("Read an unsupported value for a calculator.");
        }
    }

    public static void writeToParcel(@NonNull Parcel dest, @NonNull CharacterCalculator calculator) {

    }

    public static class CharacterState {
        public int charactersRemaining;
        public int messagesSpent;
        public int maxMessageSize;

        public CharacterState(int messagesSpent, int charactersRemaining, int maxMessageSize) {
            this.messagesSpent = messagesSpent;
            this.charactersRemaining = charactersRemaining;
            this.maxMessageSize = maxMessageSize;
        }
    }
}
