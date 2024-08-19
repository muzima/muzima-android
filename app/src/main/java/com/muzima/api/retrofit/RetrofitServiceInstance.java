package com.muzima.api.retrofit;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.muzima.utils.NetworkUtils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitServiceInstance {
    private static final OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
    private static Retrofit.Builder builder;

    public static <S> S createService(Context context, Class<S> serviceClass, String username, String password, String serverUrl) {
        if (username != null && password != null && serverUrl != null) {
            String restUrl = serverUrl+"/ws/rest/v1/";
            builder = new Retrofit.Builder()
                    .baseUrl(restUrl)
                    .addConverterFactory(buildGsonConverter())
                    .client((httpClient).build());

            String credentials = username + ":" + (password.isEmpty() ? username : password);
            final String basic = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

            httpClient.addNetworkInterceptor(chain -> {
                Request original = chain.request();

                Request.Builder requestBuilder = original.newBuilder()
                        .header("Authorization", basic)
                        .header("Accept", "application/json")
                        .method(original.method(), original.body());

                Request request = requestBuilder.build();
                return chain.proceed(request);
            });
        }

        try {
            if (NetworkUtils.isConnectedToNetwork(context)) {
                OkHttpClient client = httpClient.build();
                Retrofit retrofit = builder.client(client).build();
                return retrofit.create(serviceClass);
            } else {
                return null;
            }
        } catch (IllegalStateException ex) {
            Log.e(RetrofitServiceInstance.class.getSimpleName(), "createService: null interceptor");
            return null;
        }
    }

    private static GsonConverterFactory buildGsonConverter() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson myGson = gsonBuilder
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        return GsonConverterFactory.create(myGson);
    }
}
