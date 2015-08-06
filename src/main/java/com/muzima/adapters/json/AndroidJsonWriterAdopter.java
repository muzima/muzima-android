/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.json;

import android.util.JsonWriter;
import com.muzima.api.adapter.JsonWriterAdapter;
import org.json.JSONException;

import java.io.IOException;
import java.io.Writer;

// TODO: Need to look at this in the Android 8
public class AndroidJsonWriterAdopter implements JsonWriterAdapter {
    private JsonWriter jsonWriter;

    public AndroidJsonWriterAdopter(Writer writer) {
        this.jsonWriter = new JsonWriter(writer);
    }

    @Override
    public JsonWriterAdapter array() throws JSONException {

        try {
            jsonWriter.beginArray();
        } catch (IOException e) {
            throw new JSONException(e.getMessage());
        }
        return this;
    }

    @Override
    public JsonWriterAdapter object() throws JSONException {
        try {
            jsonWriter.beginObject();
        } catch (IOException e) {
            throw new JSONException(e.getMessage());
        }
        return this;
    }

    @Override
    public JsonWriterAdapter key(String key) throws JSONException {
        try {
            jsonWriter.name(key);
        } catch (IOException e) {
            throw new JSONException(e.getMessage());
        }
        return this;
    }

    @Override
    public JsonWriterAdapter value(String value) throws JSONException {
        try {
            jsonWriter.value(value);
        } catch (IOException e) {
            throw new JSONException(e.getMessage());
        }
        return this;
    }

    @Override
    public JsonWriterAdapter endObject() throws JSONException {
        try {
            jsonWriter.endObject();
        } catch (IOException e) {
            throw new JSONException(e.getMessage());
        }
        return this;
    }

    @Override
    public JsonWriterAdapter endArray() throws JSONException {
        try {
            jsonWriter.endArray();
        } catch (IOException e) {
            throw new JSONException(e.getMessage());
        }
        return this;
    }
}
