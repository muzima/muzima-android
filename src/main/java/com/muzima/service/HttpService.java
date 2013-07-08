package com.muzima.service;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class HttpService {
    private DefaultHttpClient httpClient;
    private int CONNECTION_TIMEOUT = 10000;
    private int SO_TIMEOUT = 10000;

    public HttpService() {
        BasicHttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, SO_TIMEOUT);
        httpClient = new DefaultHttpClient(httpParams);
    }

    public Response get(String url, Map<String, String> headers)
            throws IOException, URISyntaxException {
        URI uri = new URI(url);
        HttpGet request = new HttpGet(uri);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.setHeader(entry.getKey(), entry.getValue());
            }
        }
        return new Response(httpClient.execute(request));
    }

    public static class Response {
        private int statusCode;
        private InputStream responseBody;

        public Response(HttpResponse httpResponse) throws IOException {
            statusCode = httpResponse.getStatusLine().getStatusCode();
            responseBody = httpResponse.getEntity().getContent();
        }

        public int getStatusCode() {
            return statusCode;
        }

        public InputStream getResponseBody() {
            return responseBody;
        }
    }
}
