package com.muzima.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.muzima.Urls;
import com.muzima.db.Html5FormDataSource;
import com.muzima.domain.Html5Form;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class FormsService {
    private static final String TAG = "FORM_SERVICE";
    public static final int FETCHING = 0;
    public static final int NOT_FETCHING = 1;

    public static final int NO_NETWORK_CONNECTIVITY = 0;
    public static final int ALREADY_FETCHING = 1;
    public static final int FETCH_SUCCESSFUL = 2;
    public static final int IO_EXCEPTION = 3;
    public static final int JSON_EXCEPTION = 4;
    public static final int CONNECTION_TIMEOUT = 5;

    private Context context;
    private Html5FormDataSource html5FormDataSource;
    private int currentState;
    private OnDataFetchComplete dataFetchListener;
    private HttpService httpService;

    public FormsService(Context context, Html5FormDataSource html5FormDataSource, HttpService httpService) {
        this.context = context;
        this.html5FormDataSource = html5FormDataSource;
        this.httpService = httpService;
        currentState = NOT_FETCHING;
    }

    public void fetchForms() {
        if (isConnectedToNetwork()) {
            if (currentState == FETCHING) {
                if (dataFetchListener != null) {
                    dataFetchListener.onFetch(ALREADY_FETCHING);
                }
            }
            currentState = FETCHING;
            new FetchFormsInBackgroundTask().execute();
        } else {
            if (dataFetchListener != null) {
                dataFetchListener.onFetch(NO_NETWORK_CONNECTIVITY);
            }
        }
    }

    public int getCurrentState() {
        return currentState;
    }

    public void setDataFetchListener(OnDataFetchComplete dataFetchListener) {
        this.dataFetchListener = dataFetchListener;
    }

    private class FetchFormsInBackgroundTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                saveForms(downloadForms());
                return FETCH_SUCCESSFUL;
            } catch (ConnectTimeoutException e){
               return CONNECTION_TIMEOUT;
            } catch (IOException e) {
                return IO_EXCEPTION;
            } catch (JSONException e) {
                return JSON_EXCEPTION;
            } catch (URISyntaxException e) {
                throw new RuntimeException("URISyntaxException occurred for " + Urls.FORMS_GET_URL);
            }
        }

        @Override
        protected void onPostExecute(Integer resultCode) {
            currentState = NOT_FETCHING;
            if (dataFetchListener != null) {
                dataFetchListener.onFetch(resultCode);
            }
        }
    }

    private boolean isConnectedToNetwork() {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private String downloadForms() throws URISyntaxException, IOException {
        InputStream is = null;
        try {
            HttpService.Response response = httpService.get(Urls.FORMS_GET_URL, null);
            if(response.getStatusCode() == 408){
                throw new ConnectTimeoutException();
            }
            is = response.getResponseBody();

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
        List<Html5Form> forms = new ArrayList<Html5Form>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonForm = jsonArray.getJSONObject(i);
            String id = jsonForm.getString("id");
            String name = jsonForm.getString("name");
            String description = jsonForm.getString("description");
            description = description.equals("null") ? "" : description;
            List<String> tags = parseTagsArray(jsonForm);
            Html5Form form = new Html5Form(id, name, description, tags);
            forms.add(form);
        }
        Log.d(TAG, "Number of Forms fetched: " + forms.size());
        html5FormDataSource.deleteAllForms();
        html5FormDataSource.saveForms(forms);
    }

    private List<String> parseTagsArray(JSONObject jsonForm) throws JSONException {
        JSONArray tagsArray = jsonForm.getJSONArray("tags");
        List<String> tags = new ArrayList<String>();
        for (int i = 0; i < tagsArray.length(); i++) {
            JSONObject tag = tagsArray.getJSONObject(i);
            tags.add(tag.getString("name"));
        }
        return tags;
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

    public interface OnDataFetchComplete {
        public void onFetch(int resultCode);
    }
}
