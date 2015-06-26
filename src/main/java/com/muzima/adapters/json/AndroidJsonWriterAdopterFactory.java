package com.muzima.adapters.json;

import com.muzima.api.adapter.JsonWriterAdapter;
import com.muzima.api.adapter.JsonWriterAdapterFactory;

import java.io.Writer;

/**
 * Created by vikas on 07/01/15.
 */
public class AndroidJsonWriterAdopterFactory implements JsonWriterAdapterFactory {
    @Override
    public JsonWriterAdapter jsonWriterAdapter(Writer writer) {
        return new AndroidJsonWriterAdopter(writer);
    }
}
