package com.muzima.api;

import com.muzima.model.OpenMRSSession;

import retrofit2.Call;
import retrofit2.http.GET;

public interface OpenmrsAPIService {
    @GET("session")
    Call<OpenMRSSession> getSession();
}
