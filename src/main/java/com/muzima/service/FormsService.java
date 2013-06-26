package com.muzima.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.muzima.db.Html5FormDataSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FormsService {
    private static final String TAG = "FORM_SERVICE";
    private static final int FETCHING = 0;
    private static final int NOT_FETCHING = 1;

    public static final int NO_NETWORK_CONNECTIVITY = 0;
    public static final int ALREADY_FETCHING = 1;
    public static final int STARTED_A_NEW_FETCH = 2;

    private static final String FORMS_URL = "http://10.4.32.241:8081/openmrs-standalone/module/html5forms/forms.form";

    private Context context;
    private Html5FormDataSource mHtml5FormDataSource;
    private int currentState;

    public FormsService(Context context, Html5FormDataSource html5FormDataSource) {
        this.context = context;
        mHtml5FormDataSource = html5FormDataSource;
        currentState = NOT_FETCHING;
    }

    public int fetchForms() {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (currentState == FETCHING) {
                return ALREADY_FETCHING;
            }
            currentState = FETCHING;
            new FetchFormsInBackgroundTask().execute();
            return STARTED_A_NEW_FETCH;
        } else {
            return NO_NETWORK_CONNECTIVITY;
        }
    }

    private class FetchFormsInBackgroundTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                String formsString = downloadForms();
                saveForms(formsString);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            currentState = NOT_FETCHING;
        }
    }

    private String downloadForms() throws IOException {
        InputStream is = null;

        try {
            URL url = new URL(FORMS_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();

            int response = conn.getResponseCode();
            Log.d(TAG, "The response is: " + response);
            is = conn.getInputStream();
            String result = getStringFromInputStream(is);
            return result;

        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private void saveForms(String formsString) throws JSONException {
        JSONArray jsonArray = new JSONArray(formsString);
        for(int i =0; i<jsonArray.length(); i++){
            JSONObject jsonForm = jsonArray.getJSONObject(i);
            Log.i(TAG, "id :" + jsonForm.get("id"));
            Log.i(TAG, "name :" + jsonForm.get("name"));
            Log.i(TAG, "description :" + jsonForm.get("description"));
        }
    }

    private String getStringFromInputStream(InputStream is) throws IOException {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } finally {
            if (br != null) {
                br.close();
            }
        }
        return sb.toString();
    }
}
