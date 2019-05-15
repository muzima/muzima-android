package com.muzima.messaging.jobmanager;

import android.support.annotation.NonNull;

import androidx.work.Data;

public class SafeData {
    private final Data data;

    public SafeData(@NonNull Data data) {
        this.data = data;
    }

    public int getInt(@NonNull String key) {
        assertKeyPresence(key);
        return data.getInt(key, -1);
    }

    public long getLong(@NonNull String key) {
        assertKeyPresence(key);
        return data.getLong(key, -1);
    }

    public String getString(@NonNull String key) {
        assertKeyPresence(key);
        return data.getString(key);
    }

    public String[] getStringArray(@NonNull String key) {
        assertKeyPresence(key);
        return data.getStringArray(key);
    }

    public long[] getLongArray(@NonNull String key) {
        assertKeyPresence(key);
        return data.getLongArray(key);
    }

    public boolean getBoolean(@NonNull String key) {
        assertKeyPresence(key);
        return data.getBoolean(key, false);
    }

    private void assertKeyPresence(@NonNull String key) {
        if (!data.getKeyValueMap().containsKey(key)) {
            throw new IllegalStateException("Missing key: " + key);
        }
    }
}
