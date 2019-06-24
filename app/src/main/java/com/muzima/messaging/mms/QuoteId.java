package com.muzima.messaging.mms;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.muzima.messaging.sqlite.database.SignalAddress;

import org.json.JSONException;
import org.json.JSONObject;

public class QuoteId {
    private static final String TAG = QuoteId.class.getSimpleName();

    private static final String ID = "id";
    private static final String AUTHOR = "author";

    private final long id;
    private final SignalAddress author;

    public QuoteId(long id, @NonNull SignalAddress author) {
        this.id = id;
        this.author = author;
    }

    public long getId() {
        return id;
    }

    public @NonNull SignalAddress getAuthor() {
        return author;
    }

    public @NonNull String serialize() {
        try {
            JSONObject object = new JSONObject();
            object.put(ID, id);
            object.put(AUTHOR, author.serialize());
            return object.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize to json", e);
            return "";
        }
    }

    public static @Nullable
    QuoteId deserialize(@NonNull String serialized) {
        try {
            JSONObject json = new JSONObject(serialized);
            return new QuoteId(json.getLong(ID), SignalAddress.fromSerialized(json.getString(AUTHOR)));
        } catch (JSONException e) {
            Log.e(TAG, "Failed to deserialize from json", e);
            return null;
        }
    }
}
