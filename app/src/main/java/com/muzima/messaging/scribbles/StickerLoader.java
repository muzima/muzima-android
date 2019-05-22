package com.muzima.messaging.scribbles;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.muzima.messaging.utils.AsyncLoader;

import java.io.IOException;

public class StickerLoader extends AsyncLoader<String[]> {

    private static final String TAG = StickerLoader.class.getSimpleName();

    private final String assetDirectory;

    StickerLoader(Context context, String assetDirectory) {
        super(context);
        this.assetDirectory = assetDirectory;
    }

    @Override
    public @NonNull
    String[] loadInBackground() {
        try {
            String[] files = getContext().getAssets().list(assetDirectory);

            for (int i=0;i<files.length;i++) {
                files[i] = assetDirectory + "/" + files[i];
            }

            return files;
        } catch (IOException e) {
            Log.w(TAG, e);
        }

        return new String[0];
    }
}
