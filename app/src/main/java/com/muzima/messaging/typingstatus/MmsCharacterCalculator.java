package com.muzima.messaging.typingstatus;

import com.muzima.messaging.utils.CharacterCalculator;

public class MmsCharacterCalculator extends CharacterCalculator {

    private static final int MAX_SIZE = 5000;

    @Override
    public CharacterState calculateCharacters(String messageBody) {
        return new CharacterState(1, MAX_SIZE - messageBody.length(), MAX_SIZE);
    }
}
