package com.muzima.utils.concurrent;

import java.util.concurrent.ExecutionException;

public abstract class AssertedSuccessListener<T> implements ListenableFuture.Listener<T> {
    @Override
    public void onFailure(ExecutionException e) {
        throw new AssertionError(e);
    }
}
