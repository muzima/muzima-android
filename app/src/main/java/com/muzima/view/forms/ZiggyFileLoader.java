/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.content.res.AssetManager;
import android.webkit.JavascriptInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

public class ZiggyFileLoader {
    private String ziggyDirectoryPath;
    private AssetManager assetManager;
    private String formModelJson;

    public ZiggyFileLoader(String ziggyDirectoryPath, AssetManager assetManager, String modelJson) {
        this.ziggyDirectoryPath = ziggyDirectoryPath;
        this.assetManager = assetManager;
        formModelJson = modelJson;
    }

    @JavascriptInterface
    public String getJSFiles() throws IOException, URISyntaxException {
        StringBuilder builder = new StringBuilder();
        String[] fileNames = assetManager.list(ziggyDirectoryPath);
        for (String fileName : fileNames) {
            if (fileName.endsWith(".js")) {
                builder.append(readFileFromAssets(ziggyDirectoryPath + "/" + fileName));
            }
        }
        return builder.toString();
    }

    @JavascriptInterface
    public String loadAppData() {
        return formModelJson;
    }

    private String readFileFromAssets(String filePath) throws IOException {
        InputStream inputStream = assetManager.open(filePath);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        return readContentFromBufferedStream(bufferedReader);
    }

    private String readContentFromBufferedStream(BufferedReader input) throws IOException {
        String line;
        String eol = System.getProperty("line.separator");
        StringBuffer buffer = new StringBuffer();
        while ((line = input.readLine()) != null) {
            buffer.append(line + eol);
        }
        return buffer.toString();
    }
}
