package com.muzima.messaging.twofactoraunthentication;

import org.whispersystems.libsignal.util.guava.Optional;

public class RegistrationState {
    public enum State {
        INITIAL, VERIFYING, CHECKING, PIN
    }

    private final State   state;
    private final String  e164number;
    private final String  password;

    public State getState() {
        return state;
    }

    public String getE164number() {
        return e164number;
    }

    public String getPassword() {
        return password;
    }

    public Optional<String> getGcmToken() {
        return gcmToken;
    }

    private final Optional<String> gcmToken;

    RegistrationState(State state, String e164number, String password, Optional<String> gcmToken) {
        this.state = state;
        this.e164number = e164number;
        this.password = password;
        this.gcmToken = gcmToken;
    }

    RegistrationState(State state, RegistrationState previous) {
        this.state = state;
        this.e164number = previous.e164number;
        this.password = previous.password;
        this.gcmToken = previous.gcmToken;
    }
}
