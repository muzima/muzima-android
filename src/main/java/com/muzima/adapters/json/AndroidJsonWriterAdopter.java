/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.json;

import android.annotation.TargetApi;
import android.os.Build;
import com.muzima.api.adapter.JsonWriterAdapter;
import org.json.JSONException;
import org.json.JSONWriter;

import java.io.Writer;

// TODO: Need to look at this in the Android 8
public class AndroidJsonWriterAdopter implements JsonWriterAdapter {
    private JSONWriter jsonWriter;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public AndroidJsonWriterAdopter(Writer writer) {
        this.jsonWriter = new JSONWriter(writer);
    }

    @Override
    public JsonWriterAdapter array() throws JSONException {
        jsonWriter.array();
        return this;
    }

    @Override
    public JsonWriterAdapter object() throws JSONException {
        jsonWriter.object();
        return this;
    }

    @Override
    public JsonWriterAdapter key(String key) throws JSONException {
        jsonWriter.key(key);
        return this;
    }

    @Override
    public JsonWriterAdapter value(String value) throws JSONException {
        jsonWriter.value(value);
        return this;
    }

    @Override
    public JsonWriterAdapter endObject() throws JSONException {
        jsonWriter.endObject();
        return this;
    }

    @Override
    public JsonWriterAdapter endArray() throws JSONException {
        jsonWriter.endArray();
        return this;
    }
}
