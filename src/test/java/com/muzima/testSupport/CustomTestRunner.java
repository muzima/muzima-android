package com.muzima.testSupport;

import android.os.Build;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.internal.ActionBarSherlockNative;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;

public class CustomTestRunner extends RobolectricTestRunner {

    private static final int SDK_INT = Build.VERSION.SDK_INT;

    public CustomTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected void bindShadowClasses() {
        super.bindShadowClasses();
        ActionBarSherlock.registerImplementation(ActionBarSherlockRobolectric.class);
        ActionBarSherlock.unregisterImplementation(ActionBarSherlockNative.class);
        ActionBarSherlock.unregisterImplementation(ActionBarSherlockCompat.class);
    }



}
