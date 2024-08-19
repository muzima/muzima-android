package com.muzima.callbacks;

import com.muzima.api.retrofit.RetrofitCallback;

public interface AuntenticationCallBack  extends RetrofitCallback {
    int onAuthenticated(boolean success);
}
