package com.muzima.adapters.json;

import android.util.JsonWriter;
import com.muzima.api.adapter.JsonWriterAdapter;
import org.json.JSONException;

import java.io.IOException;
import java.io.Writer;

/**
 * Created by vikas on 07/01/15.
 */
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
            throw new JSONException(e);
        }
        return this;
    }

    @Override
    public JsonWriterAdapter object() throws JSONException {
        try {
            jsonWriter.beginObject();
        } catch (IOException e) {
            throw new JSONException(e);
        }
        return this;
    }

    @Override
    public JsonWriterAdapter key(String key) throws JSONException {
        try {
            jsonWriter.name(key);
        } catch (IOException e) {
            throw new JSONException(e);
        }
        return this;
    }

    @Override
    public JsonWriterAdapter value(String value) throws JSONException {
        try {
            jsonWriter.value(value);
        } catch (IOException e) {
            throw new JSONException(e);
        }
        return this;
    }

    @Override
    public JsonWriterAdapter endObject() throws JSONException {
        try {
            jsonWriter.endObject();
        } catch (IOException e) {
            throw new JSONException(e);
        }
        return this;
    }

    @Override
    public JsonWriterAdapter endArray() throws JSONException {
        try {
            jsonWriter.endArray();
        } catch (IOException e) {
            throw new JSONException(e);
        }
        return this;
    }
}
