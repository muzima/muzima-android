package com.muzima;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;

public class SplitInstallActivity extends AppCompatActivity implements SplitInstallStateUpdatedListener {
    public static final String FEATURE_MODULE_MODULE_NAME = "feature_module_name";
    public static final String FEATURE_MODULE_CLASS_NAME = "feature_module_class_name";

    private static final String LOG_TAG = SplitInstallActivity.class.getSimpleName();

    private SplitInstallRequest request;
    private SplitInstallManager sim;

    private String moduleName;
    private String className;

    public SplitInstallActivity() {
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        /* instance the {@link SplitInstallManager}: */
        this.sim = SplitInstallManagerFactory.create(this.getApplicationContext());

        /* obtain the feature module & class name from arguments */
        if (this.getIntent() != null) {
            Bundle extras = this.getIntent().getExtras();
            if (extras != null) {
                this.moduleName = extras.getString(FEATURE_MODULE_MODULE_NAME);
                this.className = extras.getString(FEATURE_MODULE_CLASS_NAME);
                if (this.moduleName != null && this.className != null) {
                    this.startFeatureActivity(this.moduleName, this.className);
                } else {
                    Log.e(LOG_TAG, "module and class are required.");
                }
            }
        }
    }

    /**
     * it listens for the split-install session state
     */
    @Override
    public void onStateUpdate(SplitInstallSessionState state) {
        if (state.errorCode() == SplitInstallErrorCode.NO_ERROR && state.status() == SplitInstallSessionStatus.INSTALLED) {
            Log.d(LOG_TAG, "dynamic feature " + this.moduleName + " had been installed.");
            this.startFeatureActivity(this.moduleName, this.className);
        } else {
            // this.OnSplitInstallStatus(state);
        }
    }

    /**
     * it checks if the dynamic feature module is installed and then either installs it - or starts the desired activity
     */
    private void startFeatureActivity(@NonNull String moduleName, @NonNull String className) {
        Log.d(LOG_TAG, "dynamic feature module Installed Modules: " + this.sim.getInstalledModules());
        if (this.sim.getInstalledModules().contains(moduleName)) {
            Log.d(LOG_TAG, "dynamic feature module " + moduleName + " already installed.");
            Intent intent = this.getIntent();
            intent.setClassName(BuildConfig.APPLICATION_ID, className);
            this.startActivity(intent);
            this.finish();
        } else {
            Log.d(LOG_TAG, "dynamic feature module " + moduleName + " is not installed.");
            this.installFeatureModule(moduleName);
        }
    }

    /**
     * it installs a dynamic feature module on demand
     */
    private void installFeatureModule(@NonNull String moduleName) {
        Log.d(LOG_TAG, "dynamic feature module " + moduleName + " will be installed.");
        this.request = SplitInstallRequest.newBuilder().addModule(moduleName).build();
        this.sim.registerListener(this);
        this.sim.startInstall(this.request);
    }
}
