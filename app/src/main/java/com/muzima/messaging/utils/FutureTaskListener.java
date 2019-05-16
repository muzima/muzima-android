package com.muzima.messaging.utils;

import java.util.concurrent.ExecutionException;

public interface FutureTaskListener<V> {
    public void onSuccess(V result);
    public void onFailure(ExecutionException exception);
}
