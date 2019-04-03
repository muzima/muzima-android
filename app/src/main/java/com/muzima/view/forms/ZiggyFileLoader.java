/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.content.res.AssetManager;
import android.webkit.JavascriptInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

class ZiggyFileLoader {
    private final String ziggyDirectoryPath;
    private final AssetManager assetManager;
    private final String formModelJson;

    public ZiggyFileLoader(String ziggyDirectoryPath, AssetManager assetManager, String modelJson) {
        this.ziggyDirectoryPath = ziggyDirectoryPath;
        this.assetManager = assetManager;
        formModelJson = modelJson;
    }

    @JavascriptInterface
    public String getJSFiles() throws IOException {
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
        StringBuilder buffer = new StringBuilder();
        while ((line = input.readLine()) != null) {
            buffer.append(line).append(eol);
        }
        return buffer.toString();
    }
}
