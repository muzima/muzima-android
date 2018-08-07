/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.service;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

public class JSONInputOutputToDisk extends PreferenceService{

    private static final String FILE_NAME = "IdOfPatientWithChangedUuid.txt";
    public JSONInputOutputToDisk(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        File file = context.getFileStreamPath(FILE_NAME);
        if(!file.exists()) {
            try {
                write("");
            } catch (IOException error) {
                Log.e(this.getClass().toString(), "Error thrown when initialising JSON file on device drive.", error);
            }
        }
    }

    public void add(String patientIdentifier) throws IOException {
        String savedData = read();
        List<String> savedList = deserialize(savedData);
        savedList.add(patientIdentifier);
        String updatedData = serialize(savedList);
        write(updatedData);
    }

    private void write(String dataToWrite) throws IOException {
        final File dir = new File(String.valueOf(context.getFilesDir()));
        dir.mkdirs();
        FileOutputStream fOut = context.openFileOutput(FILE_NAME,
                Context.MODE_PRIVATE);
        OutputStreamWriter osw = new OutputStreamWriter(fOut);
        osw.write(dataToWrite);
        osw.close();
    }

    private String read() throws IOException {
        FileInputStream FileInputStream = context.openFileInput(FILE_NAME);
        InputStreamReader inputStreamReader = new InputStreamReader(FileInputStream);

        StringBuilder outStringBuffer = new StringBuilder();
        String inputLine = "";
        BufferedReader inputBuffer = new BufferedReader(inputStreamReader);
        while ((inputLine = inputBuffer.readLine()) != null) {
            outStringBuffer.append(inputLine);
            outStringBuffer.append("\n");
        }
        inputBuffer.close();
        return outStringBuffer.toString();
    }

    public List readList() throws IOException {
        return deserialize(read());
    }

    public void remove(String patientIdentifier) throws IOException {
        String savedData = read();
        List<String> savedList = deserialize(savedData);
        savedList.remove(patientIdentifier);
        String updatedData = serialize(savedList);
        write(updatedData);
    }
}
