package com.muzima.messaging.webrtc;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class UncaughtExceptionHandlerManager implements Thread.UncaughtExceptionHandler {
    private final Thread.UncaughtExceptionHandler originalHandler;
    private final List<Thread.UncaughtExceptionHandler> handlers = new ArrayList<Thread.UncaughtExceptionHandler>();
    private final String TAG = UncaughtExceptionHandlerManager.class.getSimpleName();

    public UncaughtExceptionHandlerManager() {
        originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        registerHandler(originalHandler);
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public void registerHandler(Thread.UncaughtExceptionHandler handler) {
        handlers.add(handler);
    }

    public void unregister() {
        Thread.setDefaultUncaughtExceptionHandler(originalHandler);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        for (int i = handlers.size() - 1; i >= 0; i--) {
            try {
                handlers.get(i).uncaughtException(thread, throwable);
            } catch(Throwable t) {
                Log.e(TAG, "Error in uncaught exception handling", t);
            }
        }
    }
}
