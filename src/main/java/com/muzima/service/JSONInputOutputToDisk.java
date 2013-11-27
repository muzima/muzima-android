package com.muzima.service;

import android.content.Context;
import android.util.Log;

import java.io.*;
import java.util.List;

public class JSONInputOutputToDisk extends PreferenceService{

    public static final String FILE_NAME = "IdOfPatientWithChangedUuid.txt";
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
                Log.e(this.getClass().toString(), "Error thrown when initialising JSON file on device drive.");
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
                Context.MODE_WORLD_READABLE);
        OutputStreamWriter osw = new OutputStreamWriter(fOut);
        osw.write(dataToWrite);
        osw.close();
    }

    private String read() throws IOException {
        FileInputStream FileInputStream = context.openFileInput(FILE_NAME);
        InputStreamReader inputStreamReader = new InputStreamReader(FileInputStream);

        StringBuffer outStringBuffer = new StringBuffer();
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
